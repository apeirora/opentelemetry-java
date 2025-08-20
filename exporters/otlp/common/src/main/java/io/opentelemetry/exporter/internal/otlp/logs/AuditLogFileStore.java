/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.internal.otlp.logs;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.AuditLogStore;
import io.opentelemetry.sdk.resources.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * A file-based implementation of {@link AuditLogStore} that provides thread-safe concurrent reading
 * and writing of audit log records to/from the file system.
 */
public final class AuditLogFileStore implements AuditLogStore {

  private static final String LOG_FILE_EXTENSION = ".log";
  private static final String DEFAULT_LOG_FILE_NAME = "audit" + LOG_FILE_EXTENSION;
  // private static final DateTimeFormatter TIMESTAMP_FORMATTER =
  // DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
  // .withZone(ZoneId.of("UTC"));

  private final Path logFilePath;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Set<String> loggedRecords = ConcurrentHashMap.newKeySet();

  /**
   * Creates a new AuditLogFileStore that stores logs in the specified file.
   *
   * @param filePath the path to the log file
   * @throws IOException if the file cannot be created or accessed
   */
  public AuditLogFileStore(String filePath) throws IOException {
    this(Paths.get(filePath));
  }

  /**
   * Creates a new AuditLogFileStore that stores logs in the specified path. If the path is a
   * directory, logs will be stored in a default file within that directory. If the path is a file,
   * logs will be stored directly in that file.
   *
   * @param path the path to the log file or directory
   * @throws IOException if the file or directory cannot be created or accessed
   */
  public AuditLogFileStore(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      this.logFilePath = path.resolve(DEFAULT_LOG_FILE_NAME);
    } else {
      this.logFilePath = path;
    }

    // Ensure parent directories exist
    if (logFilePath.getParent() != null) {
      Files.createDirectories(logFilePath.getParent());
    }

    // Create the file if it doesn't exist
    if (!Files.exists(logFilePath)) {
      Files.createFile(logFilePath);
    }

    // Load existing log record IDs to avoid duplicates
    loadExistingRecordIds();
  }

  @Override
  public void save(LogRecordData logRecord) throws IOException {
    String recordId = generateRecordId(logRecord);

    // Check if we've already logged this record
    if (loggedRecords.contains(recordId)) {
      return;
    }

    lock.writeLock().lock();
    try {

      LogMarshaler logMarshaler = LogMarshaler.create(logRecord);

      // write to the log file using an output stream
      logMarshaler.writeJsonTo(
          Files.newOutputStream(logFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND));

      loggedRecords.add(recordId);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void remove(Collection<LogRecordData> logs) {
    lock.writeLock().lock();
    try {
      Set<String> recordIdsToRemove = new HashSet<>();
      for (LogRecordData log : logs) {
        recordIdsToRemove.add(generateRecordId(log));
      }

      // Read all lines, filter out the ones to remove, then write back
      Collection<String> remainingLines = new ArrayList<>();
      try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
        String line;
        while ((line = reader.readLine()) != null) {
          String recordId = extractRecordIdFromLine(line);
          if (recordId == null || !recordIdsToRemove.contains(recordId)) {
            remainingLines.add(line);
          } else {
            loggedRecords.remove(recordId);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to read log file for removal", e);
      }

      // Write the remaining lines back to the file
      try (BufferedWriter writer =
          Files.newBufferedWriter(
              logFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        for (String line : remainingLines) {
          writer.write(line);
          writer.newLine();
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to write log file after removal", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Collection<LogRecordData> getAll() {
    lock.readLock().lock();
    try {
      Collection<LogRecordData> records = new ArrayList<>();
      try (Stream<String> lines = Files.lines(logFilePath)) {
        lines.forEach(
            line -> {
              LogRecordData record = parseLogRecord(line);
              if (record != null) {
                records.add(record);
              }
            });
      } catch (IOException e) {
        throw new RuntimeException("Failed to read log file", e);
      }
      return records;
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Loads existing record IDs from the log file to prevent duplicates. */
  private void loadExistingRecordIds() throws IOException {
    if (!Files.exists(logFilePath) || Files.size(logFilePath) == 0) {
      return;
    }

    lock.readLock().lock();
    try (Stream<String> lines = Files.lines(logFilePath)) {
      lines.forEach(
          line -> {
            String recordId = extractRecordIdFromLine(line);
            if (recordId != null) {
              loggedRecords.add(recordId);
            }
          });
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Generates a unique ID for a log record based on its content. */
  private String generateRecordId(LogRecordData logRecord) {
    return String.valueOf(
        (logRecord.getTimestampEpochNanos()
                + String.valueOf(logRecord.getBodyValue())
                + logRecord.getSeverity().toString())
            .hashCode());
  }

  /** Extracts the record ID from a log line. */
  private String extractRecordIdFromLine(String line) {
    if (line.startsWith("[") && line.contains("]")) {
      int endIndex = line.indexOf("]");
      return line.substring(1, endIndex);
    }
    return null;
  }

  /**
   * Parses a log record from a stored line (simplified implementation). In a production system, you
   * might want to use JSON or another structured format.
   */
  private LogRecordData parseLogRecord(String line) {
    try {

      // TODO read json from file, unmarshal it into a LogRecordData object

      return createLogRecordData();

    } catch (Exception e) {
      // If parsing fails, return null (could log this in a real implementation)
      return null;
    }
  }

  /** Creates a basic LogRecordData implementation for parsed records. */
  private LogRecordData createLogRecordData() {

    return new LogRecordData() {

      @Override
      public int getTotalAttributeCount() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public long getTimestampEpochNanos() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public SpanContext getSpanContext() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getSeverityText() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Severity getSeverity() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Resource getResource() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public long getObservedTimestampEpochNanos() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public InstrumentationScopeInfo getInstrumentationScopeInfo() {
        // TODO Auto-generated method stub
        return null;
      }

      @SuppressWarnings("deprecation")
      @Override
      public Body getBody() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Attributes getAttributes() {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }
}
