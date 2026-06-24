/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface for hooking into the audit record pipeline for enrichment and forwarding.
 *
 * <p>Processors MUST only add attributes to records (enrichment). They MUST NOT remove mandatory
 * fields, filter records, aggregate records, or introduce sampling. Processors that would remove or
 * filter records are rejected at configuration time by {@link SdkAuditProvider}.
 */
@ThreadSafe
public interface AuditRecordProcessor extends Closeable {

  /**
   * Returns a composite {@link AuditRecordProcessor} that delegates to all given processors in
   * order.
   */
  static AuditRecordProcessor composite(AuditRecordProcessor... processors) {
    return composite(Arrays.asList(processors));
  }

  /**
   * Returns a composite {@link AuditRecordProcessor} that delegates to all given processors in
   * order.
   */
  static AuditRecordProcessor composite(Iterable<AuditRecordProcessor> processors) {
    List<AuditRecordProcessor> list = new ArrayList<>();
    for (AuditRecordProcessor p : processors) {
      list.add(p);
    }
    if (list.size() == 1) {
      return list.get(0);
    }
    return MultiAuditRecordProcessor.create(list);
  }

  /**
   * Called synchronously on the calling thread after the record has been enqueued and before the
   * {@link io.opentelemetry.api.audit.AuditReceipt} is returned to the caller.
   *
   * <p>Implementations MAY enrich {@code record} by adding attributes. They MUST NOT block
   * indefinitely.
   *
   * @param context the ambient {@link Context} at the time of {@code emit()}
   * @param record the mutable record; enrichment only
   */
  void onEmit(Context context, ReadWriteAuditRecord record);

  /** Shuts down this processor, flushing all buffered records. */
  default CompletableResultCode shutdown() {
    return forceFlush();
  }

  /** Requests that all buffered records be exported as soon as possible. */
  default CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  default void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }
}
