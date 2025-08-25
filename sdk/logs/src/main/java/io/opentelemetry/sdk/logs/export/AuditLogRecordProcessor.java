/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

/** An implementation of {@link LogRecordProcessor} that processes logs for auditing purposes. */
public final class AuditLogRecordProcessor implements LogRecordProcessor {

  /**
   * Returns a new Builder for {@link AuditLogRecordProcessor}.
   *
   * @param logRecordExporter the {@link LogRecordExporter} to which the Logs are pushed.
   *     OtlpGrpcLogRecordExporter is a good choice.
   * @return a new {@link AuditLogRecordProcessor}.
   * @throws NullPointerException if the {@code logRecordExporter} is {@code null}.
   */
  public static AuditLogRecordProcessorBuilder builder(
      LogRecordExporter logRecordExporter, AuditLogStore logStore) {
    return new AuditLogRecordProcessorBuilder(logRecordExporter, logStore);
  }

  /** The exporter to export logs. OtlpGrpcLogRecordExporter is recommended. */
  private final LogRecordExporter exporter;

  /** The exception handler to handle exceptions during log export. */
  @Nullable private final AuditExceptionHandler handler;

  @Nullable private CompletableResultCode lastResultCode;

  /** The persistent storage for logs. */
  private final AuditLogStore persistency;

  /** The PriorityBlockingQueue to hold the logs before exporting. */
  private final Queue<LogRecordData> queue;

  /** A flag to indicate whether the processor is shutdown. */
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  /** The maximum number of logs to export in a single batch. */
  private final int size;

  /** The timeout for exporting logs in nanoseconds. */
  private final long timeout;

  /**
   * Creates a new AuditLogRecordProcessor with the given parameters.
   *
   * @param logRecordExporter the {@link LogRecordExporter} to which the Logs are pushed.
   * @param exceptionHandler the {@link AuditExceptionHandler} to handle exceptions during log
   *     export.
   * @param scheduleDelayNanos the delay in nanoseconds between periodic exports.
   * @param maxExportBatchSize the maximum number of logs to export in a single batch.
   * @param exporterTimeoutNanos the timeout for exporting logs in nanoseconds.
   */
  AuditLogRecordProcessor(
      LogRecordExporter logRecordExporter,
      @Nullable AuditExceptionHandler exceptionHandler,
      AuditLogStore logStore,
      long scheduleDelayNanos,
      int maxExportBatchSize,
      long exporterTimeoutNanos) {
    exporter = logRecordExporter;
    size = maxExportBatchSize;
    timeout = exporterTimeoutNanos;
    handler = exceptionHandler;
    queue =
        new PriorityBlockingQueue<>(
            maxExportBatchSize,
            (record1, record2) -> {
              // compare by severity, higher severity first
              return Integer.compare(
                  record2.getSeverity().getSeverityNumber(),
                  record1.getSeverity().getSeverityNumber());
            });
    persistency = logStore;

    // Get all logs from persistent storage and add them to the queue
    queue.addAll(persistency.getAll());
    exportLogs(); // export logs immediately to ensure no logs are missed

    scheduler = Executors.newSingleThreadScheduledExecutor();
    future =
        scheduler.scheduleAtFixedRate(
            () -> {
              exportLogs(); // export logs periodically, regardless of the queue size
            },
            scheduleDelayNanos,
            scheduleDelayNanos,
            TimeUnit.NANOSECONDS);
  }

  private final ScheduledFuture<?> future;
  private final ScheduledExecutorService scheduler;

  /**
   * Exports logs from the queue to the exporter. If the queue is empty, it does nothing. If the
   * export fails, it retries exporting logs and handles exceptions using the provided handler.
   */
  void exportLogs() {
    if (queue.isEmpty()) {
      return;
    }
    Collection<CompletableResultCode> results = new ArrayList<>();
    Collection<LogRecordData> failedExportlogs = new ArrayList<>();
    // AuditException auditException = new AuditException("Exporting logs failed");
    while (!queue.isEmpty()) {
      // Create a collection of LogRecordData from the queue with a maximum size
      // TODO: comes with newer Java version: Collection<LogRecordData> logList =
      // queue.stream().limit(size).toList();

      Object[] arr = queue.stream().limit(size).toArray();
      Collection<LogRecordData> logs = new ArrayList<>(arr.length);
      for (Object o : arr) {
        logs.add((LogRecordData) o);
      }

      try {
        CompletableResultCode export = exporter.export(logs);
        lastResultCode = export;

        export.whenComplete(
            () -> {
              if (export.isSuccess()) {
                persistency.removeAll(logs);
              } else {
                failedExportlogs.addAll(logs);

                Throwable exportFailure =
                    export.getFailureThrowable(); // TODO retry exporting logs - here (A)?
                if (exportFailure != null) {
                  if (exportFailure instanceof RuntimeException) {
                    throw (RuntimeException) exportFailure;
                  }
                  throw new AuditException(exportFailure.getMessage(), exportFailure, logs);
                }
                // TODO retry exporting logs
                // auditException.because(export.getFailureThrowable());
              }
            });
        // results.add(export.join(timeout, TimeUnit.NANOSECONDS));
        results.add(export); // don't block here, let the export complete asynchronously

      } catch (AuditException e) {
        results.add(CompletableResultCode.ofExceptionalFailure(e));
        if (handler != null) {
          handler.handle(e);
        } else {
          throw e;
        }

      } catch (RuntimeException e) {
        // TODO retry exporting logs - or here (B)?
        results.add(CompletableResultCode.ofExceptionalFailure(e));

        // queue.clear(); // FIXME: Clear the queue to avoid reprocessing the same logs
        if (handler != null) {
          handler.handle(new AuditException(e.getMessage(), e, logs));
        } else {
          throw new AuditException(e.getMessage(), e, logs);
        }
      } finally {
        queue.removeAll(logs);
      }
    }

    CompletableResultCode all = CompletableResultCode.ofAll(results);
    lastResultCode = all;
    // all.join(timeout * results.size(), TimeUnit.NANOSECONDS);
    if (all.isDone() && !all.isSuccess()) {
      Throwable e = all.getFailureThrowable();
      String msg = e != null ? e.getMessage() : "Exporting logs failed";
      if (handler != null) {
        handler.handle(new AuditException(msg, e, failedExportlogs));
      } else {
        // If no handler is provided, we throw an exception
        throw new AuditException(msg, e, failedExportlogs);
      }
    }
  }

  /**
   * Exports logs immediately, regardless of the queue size. This is useful for flushing logs when
   * the processor is shutdown.
   *
   * @return {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode forceFlush() {
    exportLogs();
    return lastResultCode != null ? lastResultCode : CompletableResultCode.ofSuccess();
  }

  /**
   * Returns the last export operation ({@link #exportLogs()}) result. If the processor was never
   * triggered, it returns <code>null</code>. Useful to wait for the last export to finish via
   * {@link CompletableResultCode#join(long, TimeUnit)}.
   *
   * @return CompletableResultCode from {@link LogRecordExporter#export(Collection)} of {@link
   *     #exporter}.
   */
  @Nullable
  public CompletableResultCode getLastResultCode() {
    return lastResultCode;
  }

  /**
   * Accepts a log record and adds it to the queue. If the processor is shutdown, it throws an
   * exception or calls the handler.
   *
   * @param context the context of the log record.
   * @param logRecord the log record to be processed.
   */
  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {
    if (logRecord == null) {
      return;
    }

    if (shutdown.get()) {
      AuditException exception =
          new AuditException(
              new IllegalStateException(
                  "AuditLogRecordProcessor is shutdown, cannot accept new logs."),
              context,
              Collections.singletonList(logRecord.toLogRecordData()));
      if (handler != null) {
        handler.handle(exception);
      } else {
        throw exception;
      }
    }

    try {
      LogRecordData data = logRecord.toLogRecordData();
      persistency.save(data);
      queue.add(data);
    } catch (IOException e) {
      AuditException exception =
          new AuditException(e, context, Collections.singletonList(logRecord.toLogRecordData()));
      if (handler != null) {
        handler.handle(exception);
      } else {
        throw exception;
      }
    }

    if (queue.size() >= size) {
      // when we have reached certain size, we export logs immediately
      exportLogs();
    }
  }

  /**
   * Shuts down the processor. This method will export all remaining logs in the queue before
   * shutting down. If this method is called multiple times, it will only export logs once.
   *
   * @return {@link CompletableResultCode#ofSuccess()}.
   */
  @Override
  public CompletableResultCode shutdown() {
    if (!shutdown.getAndSet(true)) {
      // First time shutdown is called, we export all remaining logs
      future.cancel(false);
      scheduler.shutdown();
      return forceFlush();
    }
    return lastResultCode != null ? lastResultCode : CompletableResultCode.ofSuccess();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AuditLogRecordProcessor [exporter=");
    builder.append(exporter);
    builder.append(", handler=");
    builder.append(handler);
    builder.append(", queue=");
    builder.append(queue);
    builder.append(", shutdown=");
    builder.append(shutdown);
    builder.append(", size=");
    builder.append(size);
    builder.append(", timeout=");
    builder.append(timeout);
    builder.append(", persistency=");
    builder.append(persistency);
    builder.append("]");
    return builder.toString();
  }
}
