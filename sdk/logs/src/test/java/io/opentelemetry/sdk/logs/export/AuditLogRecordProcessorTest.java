/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.internal.testing.slf4j.SuppressLogger;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorTest.WaitingLogRecordExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@SuppressWarnings("PreferJavaTimeOverload")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogRecordProcessorTest {

  private static final String LOG_MESSAGE_1 = "Hello audit world 1!";
  private static final String LOG_MESSAGE_2 = "Hello audit world 2!";
  private static final long MAX_SCHEDULE_DELAY_MILLIS = 500;

  @Mock private LogRecordExporter mockLogRecordExporter;
  @Mock private AuditExceptionHandler mockExceptionHandler;
  @Mock private AuditLogStore mockLogStore;

  @BeforeEach
  void setUp() {
    when(mockLogRecordExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    when(mockLogStore.getAll()).thenReturn(new ArrayList<>());
  }

  private void emitLog(SdkLoggerProvider sdkLoggerProvider, String message) {
    sdkLoggerProvider
        .loggerBuilder(getClass().getName())
        .build()
        .logRecordBuilder()
        .setBody(message)
        .emit();
  }

  @Test
  void builderDefaults() {
    AuditLogRecordProcessorBuilder builder =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore);
    assertThat(builder.getScheduleDelayNanos())
        .isEqualTo(
            TimeUnit.MILLISECONDS.toNanos(
                AuditLogRecordProcessorBuilder.DEFAULT_SCHEDULE_DELAY_MILLIS));
    assertThat(builder.getMaxExportBatchSize())
        .isEqualTo(AuditLogRecordProcessorBuilder.DEFAULT_MAX_EXPORT_BATCH_SIZE);
    assertThat(builder.getExporterTimeoutNanos())
        .isEqualTo(
            TimeUnit.MILLISECONDS.toNanos(
                AuditLogRecordProcessorBuilder.DEFAULT_EXPORT_TIMEOUT_MILLIS));
  }

  @Test
  void builderInvalidConfig() {
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setScheduleDelay(-1, TimeUnit.MILLISECONDS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("delay must be non-negative");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setScheduleDelay(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("unit");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setScheduleDelay(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("delay");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setExporterTimeout(-1, TimeUnit.MILLISECONDS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("timeout must be non-negative");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setExporterTimeout(1, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("unit");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setExporterTimeout(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("timeout");
    assertThatThrownBy(
            () ->
                AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
                    .setMaxExportBatchSize(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxExportBatchSize must be positive.");
  }

  @Test
  void emitMultipleLogs() throws IOException {
    WaitingLogRecordExporter waitingLogRecordExporter =
        new WaitingLogRecordExporter(2, CompletableResultCode.ofSuccess());
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    SdkLoggerProvider loggerProvider =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                AuditLogRecordProcessor.builder(waitingLogRecordExporter, logStore)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    emitLog(loggerProvider, LOG_MESSAGE_1);
    emitLog(loggerProvider, LOG_MESSAGE_2);
    List<LogRecordData> exported = waitingLogRecordExporter.waitForExport();
    assertThat(exported)
        .satisfiesExactly(
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_1),
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_2));
  }

  @Test
  void emitMoreLogsThanBufferSize() throws IOException {
    CompletableLogRecordExporter logRecordExporter = new CompletableLogRecordExporter();
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                AuditLogRecordProcessor.builder(logRecordExporter, logStore)
                    .setMaxExportBatchSize(2)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);

    logRecordExporter.succeed();

    await()
        .untilAsserted(
            () ->
                assertThat(logRecordExporter.getExported())
                    .hasSize(6)
                    .allSatisfy(logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_1)));
  }

  @Test
  void forceFlush() throws IOException {
    WaitingLogRecordExporter waitingLogRecordExporter =
        new WaitingLogRecordExporter(100, CompletableResultCode.ofSuccess(), 1);
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    AuditLogRecordProcessor auditLogRecordProcessor =
        AuditLogRecordProcessor.builder(waitingLogRecordExporter, logStore)
            .setMaxExportBatchSize(49)
            .setScheduleDelay(10, TimeUnit.SECONDS)
            .build();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder().addLogRecordProcessor(auditLogRecordProcessor).build();

    for (int i = 0; i < 50; i++) {
      emitLog(sdkLoggerProvider, "notExported");
    }
    List<LogRecordData> exported = waitingLogRecordExporter.waitForExport();
    assertThat(exported).isNotNull();
    assertThat(exported.size()).isEqualTo(49);

    for (int i = 0; i < 50; i++) {
      emitLog(sdkLoggerProvider, "notExported");
    }
    exported = waitingLogRecordExporter.waitForExport();
    assertThat(exported).isNotNull();
    assertThat(exported.size()).isEqualTo(49);

    auditLogRecordProcessor.forceFlush().join(10, TimeUnit.SECONDS);
    exported = waitingLogRecordExporter.getExported();
    assertThat(exported).isNotNull();
    assertThat(exported.size()).isEqualTo(2);
  }

  @Test
  void emitLogsToMultipleExporters() throws IOException {
    WaitingLogRecordExporter waitingLogRecordExporter1 =
        new WaitingLogRecordExporter(2, CompletableResultCode.ofSuccess());
    WaitingLogRecordExporter waitingLogRecordExporter2 =
        new WaitingLogRecordExporter(2, CompletableResultCode.ofSuccess());
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                AuditLogRecordProcessor.builder(
                        LogRecordExporter.composite(
                            Arrays.asList(waitingLogRecordExporter1, waitingLogRecordExporter2)),
                        logStore)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);
    emitLog(sdkLoggerProvider, LOG_MESSAGE_2);
    List<LogRecordData> exported1 = waitingLogRecordExporter1.waitForExport();
    List<LogRecordData> exported2 = waitingLogRecordExporter2.waitForExport();
    assertThat(exported1)
        .hasSize(2)
        .satisfiesExactly(
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_1),
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_2));
    assertThat(exported2)
        .hasSize(2)
        .satisfiesExactly(
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_1),
            logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_2));
  }

  @Test
  void ignoresNullLogs() throws IOException {
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();
    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, logStore).build();
    try {
      assertThatCode(() -> processor.onEmit(null, null)).doesNotThrowAnyException();
    } finally {
      processor.shutdown();
    }
  }

  @Test
  @SuppressLogger(MultiLogRecordExporter.class)
  void exporterThrowsException() throws IOException {
    WaitingLogRecordExporter waitingLogRecordExporter =
        new WaitingLogRecordExporter(1, CompletableResultCode.ofSuccess());
    doThrow(new IllegalArgumentException("No export for you."))
        .when(mockLogRecordExporter)
        .export(anyList());
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                AuditLogRecordProcessor.builder(
                        LogRecordExporter.composite(
                            Arrays.asList(mockLogRecordExporter, waitingLogRecordExporter)),
                        logStore)
                    .setScheduleDelay(MAX_SCHEDULE_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                    .build())
            .build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);

    List<LogRecordData> exported = waitingLogRecordExporter.waitForExport();
    assertThat(exported)
        .satisfiesExactly(logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_1));
    waitingLogRecordExporter.reset();
    // Continue to export after the exception was received.
    emitLog(sdkLoggerProvider, LOG_MESSAGE_2);
    exported = waitingLogRecordExporter.waitForExport();
    assertThat(exported)
        .satisfiesExactly(logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_2));
  }

  @Test
  void exceptionHandlerCalledOnStorageFailure() throws IOException {
    doThrow(new IOException("Storage failed")).when(mockLogStore).save(any());

    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, mockLogStore)
            .setExceptionHandler(mockExceptionHandler)
            .build();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder().addLogRecordProcessor(processor).build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);

    verify(mockExceptionHandler, times(1)).handle(any(AuditException.class));
  }

  @Test
  void shutdownAfterEmitThrowsException() throws IOException {
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();
    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, logStore)
            .setExceptionHandler(mockExceptionHandler)
            .build();

    processor.shutdown();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder().addLogRecordProcessor(processor).build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_1);

    verify(mockExceptionHandler, times(1)).handle(any(AuditException.class));
  }

  @Test
  @Timeout(10)
  void shutdownFlushes() throws IOException {
    WaitingLogRecordExporter waitingLogRecordExporter =
        new WaitingLogRecordExporter(1, CompletableResultCode.ofSuccess());
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();

    SdkLoggerProvider sdkLoggerProvider =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                AuditLogRecordProcessor.builder(waitingLogRecordExporter, logStore)
                    .setScheduleDelay(10, TimeUnit.SECONDS)
                    .build())
            .build();

    emitLog(sdkLoggerProvider, LOG_MESSAGE_2);

    // Force a shutdown, which forces processing of all remaining logs.
    sdkLoggerProvider.shutdown().join(10, TimeUnit.SECONDS);

    List<LogRecordData> exported = waitingLogRecordExporter.getExported();
    assertThat(exported)
        .satisfiesExactly(logRecordData -> assertThat(logRecordData).hasBody(LOG_MESSAGE_2));
  }

  @Test
  void shutdownPropagatesSuccess() throws IOException {
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();
    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, logStore).build();
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void shutdownPropagatesFailure() throws IOException {
    when(mockLogRecordExporter.shutdown()).thenReturn(CompletableResultCode.ofFailure());
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();
    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, logStore).build();
    CompletableResultCode result = processor.shutdown();
    result.join(1, TimeUnit.SECONDS);
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  void toString_Valid() throws IOException {
    when(mockLogRecordExporter.toString()).thenReturn("MockLogRecordExporter");
    InMemoryAuditLogStore logStore = new InMemoryAuditLogStore();
    AuditLogRecordProcessor processor =
        AuditLogRecordProcessor.builder(mockLogRecordExporter, logStore).build();

    String result = processor.toString();
    assertThat(result).contains("AuditLogRecordProcessor");
    assertThat(result).contains("MockLogRecordExporter");
  }

  // Helper classes similar to BatchLogRecordProcessorTest

  private static class CompletableLogRecordExporter implements LogRecordExporter {

    private final List<CompletableResultCode> results = new ArrayList<>();
    private final List<LogRecordData> exported = new ArrayList<>();
    private volatile boolean succeeded;

    List<LogRecordData> getExported() {
      return exported;
    }

    void succeed() {
      succeeded = true;
      results.forEach(CompletableResultCode::succeed);
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
      exported.addAll(logs);
      if (succeeded) {
        return CompletableResultCode.ofSuccess();
      }
      CompletableResultCode result = new CompletableResultCode();
      results.add(result);
      return result;
    }

    @Override
    public CompletableResultCode flush() {
      if (succeeded) {
        return CompletableResultCode.ofSuccess();
      } else {
        return CompletableResultCode.ofFailure();
      }
    }

    @Override
    public CompletableResultCode shutdown() {
      return flush();
    }
  }

  // Simple in-memory implementation for testing
  private static class InMemoryAuditLogStore implements AuditLogStore {
    private final List<LogRecordData> logs = new ArrayList<>();

    @Override
    public void save(LogRecordData logRecord) throws IOException {
      logs.add(logRecord);
    }

    @Override
    public void remove(Collection<LogRecordData> logs) {
      this.logs.removeAll(logs);
    }

    @Override
    public Collection<LogRecordData> getAll() {
      return new ArrayList<>(logs);
    }
  }
}
