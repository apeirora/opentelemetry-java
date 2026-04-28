/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Transmits {@link AuditRecordData}s to the configured audit sink.
 *
 * <p>Implementations MUST document their concurrency characteristics. {@link #export} MUST NOT be
 * called concurrently on the same instance.
 *
 * <p>The audit logging specification prohibits partial success: if the receiver cannot process one
 * or more records, the entire batch MUST be rejected. Implementations MUST treat a partial-success
 * response from the OTLP receiver as a hard failure and retry the full batch.
 */
public interface AuditRecordExporter extends Closeable {

  /**
   * Exports the given collection of {@link AuditRecordData}s to the audit sink.
   *
   * <p>MUST NOT be called concurrently on the same exporter instance. MUST NOT block indefinitely;
   * the exporter MUST time out within the configured export timeout.
   *
   * @param records the records to export; the collection MUST NOT be mutated after this call
   * @return an {@link AuditExportResult} containing one {@link
   *     io.opentelemetry.api.audit.AuditReceipt} per record on success, or a failure result
   */
  AuditExportResult export(Collection<AuditRecordData> records);

  /**
   * Requests that any internally buffered records be exported immediately.
   *
   * @return a result indicating whether the flush succeeded
   */
  CompletableResultCode flush();

  /**
   * Shuts down this exporter. On the first call, flushes any buffered records and releases all
   * resources. Subsequent calls are no-ops.
   *
   * @return a result indicating whether the shutdown succeeded
   */
  CompletableResultCode shutdown();

  @Override
  default void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }
}
