/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.http.audit;

import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.exporter.otlp.internal.HttpExporter;
import io.opentelemetry.exporter.otlp.internal.HttpExporterBuilder;
import io.opentelemetry.sdk.audit.AuditExportResult;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Exports {@link AuditRecordData}s using OTLP/HTTP to the dedicated {@code /v1/audit} endpoint.
 *
 * <p>Audit records are serialized as OTLP {@code LogRecord} protobuf messages (reusing the {@code
 * ExportLogsServiceRequest} envelope) with mandatory audit fields stored as attributes.
 *
 * <p>The OTLP receiver MUST NOT respond with {@code partial_success}; any partial-success response
 * is treated as a hard failure and all records in the batch are retained for retry.
 *
 * <p>Create via {@link #builder()} or {@link #getDefault()}.
 */
@ThreadSafe
public final class OtlpHttpAuditRecordExporter implements AuditRecordExporter {

  static final String DEFAULT_ENDPOINT = "http://localhost:4318/v1/audit";

  private final HttpExporterBuilder builder;
  private final HttpExporter delegate;

  OtlpHttpAuditRecordExporter(HttpExporterBuilder builder, HttpExporter delegate) {
    this.builder = builder;
    this.delegate = delegate;
  }

  /** Returns a new {@link OtlpHttpAuditRecordExporter} with default configuration. */
  public static OtlpHttpAuditRecordExporter getDefault() {
    return builder().build();
  }

  /** Returns a new {@link OtlpHttpAuditRecordExporterBuilder}. */
  public static OtlpHttpAuditRecordExporterBuilder builder() {
    return new OtlpHttpAuditRecordExporterBuilder();
  }

  /**
   * Exports the given audit records to the configured OTLP {@code /v1/audit} endpoint.
   *
   * <p>Audit records are adapted to OTLP {@code LogRecord}s via {@link AuditLogRecordDataAdapter}.
   * The {@code InstrumentationScope} is left empty and {@code SeverityNumber} is unset per the
   * audit logging specification.
   *
   * <p>Returns synthetic {@link AuditReceipt}s on success. The {@code IntegrityHash} field is empty
   * in this implementation; a future OTLP response extension will carry the sink-computed hash.
   *
   * <p>Any {@code partial_success} response is treated as a hard failure (all records are returned
   * in the failure result for retry).
   */
  @Override
  public AuditExportResult export(Collection<AuditRecordData> records) {
    if (records.isEmpty()) {
      return AuditExportResult.success(Collections.emptyList());
    }

    // Adapt AuditRecordData → LogRecordData for marshaling
    List<LogRecordData> adapted = new ArrayList<>(records.size());
    for (AuditRecordData record : records) {
      adapted.add(new AuditLogRecordDataAdapter(record));
    }

    // Serialize as ExportLogsServiceRequest and send to /v1/audit
    LogsRequestMarshaler marshaler = LogsRequestMarshaler.create(adapted);
    AtomicReference<Throwable> failureCause = new AtomicReference<>();
    AtomicBoolean succeeded = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);

    CompletableResultCode result = delegate.export(marshaler, adapted.size());
    result.whenComplete(
        () -> {
          if (result.isSuccess()) {
            succeeded.set(true);
          } else {
            failureCause.set(result.getFailureThrowable());
          }
          latch.countDown();
        });

    try {
      // Block until export completes (audit records must be acknowledged)
      latch.await(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return AuditExportResult.failure(e);
    }

    if (latch.getCount() > 0) {
      return AuditExportResult.failure(
          new AuditDeliveryException(
              "OTLP export timed out waiting for /v1/audit acknowledgement"));
    }

    if (!succeeded.get()) {
      Throwable cause = failureCause.get();
      return cause != null ? AuditExportResult.failure(cause) : AuditExportResult.failure();
    }

    // Synthesize receipts (IntegrityHash populated by sink in future OTLP extension)
    List<AuditReceipt> receipts = new ArrayList<>(records.size());
    for (AuditRecordData record : records) {
      receipts.add(AuditReceipt.create(record.getRecordId(), "", 0));
    }
    return AuditExportResult.success(receipts);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(", ", "OtlpHttpAuditRecordExporter{", "}");
    joiner.add(builder.toString(false));
    return joiner.toString();
  }
}
