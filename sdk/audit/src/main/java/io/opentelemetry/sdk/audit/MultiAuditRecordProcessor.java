/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.ArrayList;
import java.util.List;

/** Composite {@link AuditRecordProcessor} that delegates to multiple processors in order. */
final class MultiAuditRecordProcessor implements AuditRecordProcessor {

  private final List<AuditRecordProcessor> processors;

  private MultiAuditRecordProcessor(List<AuditRecordProcessor> processors) {
    this.processors = processors;
  }

  static MultiAuditRecordProcessor create(List<AuditRecordProcessor> processors) {
    return new MultiAuditRecordProcessor(new ArrayList<>(processors));
  }

  @Override
  public void onEmit(Context context, ReadWriteAuditRecord record) {
    for (AuditRecordProcessor processor : processors) {
      processor.onEmit(context, record);
    }
  }

  @Override
  public CompletableResultCode shutdown() {
    List<CompletableResultCode> results = new ArrayList<>(processors.size());
    for (AuditRecordProcessor processor : processors) {
      results.add(processor.shutdown());
    }
    return CompletableResultCode.ofAll(results);
  }

  @Override
  public CompletableResultCode forceFlush() {
    List<CompletableResultCode> results = new ArrayList<>(processors.size());
    for (AuditRecordProcessor processor : processors) {
      results.add(processor.forceFlush());
    }
    return CompletableResultCode.ofAll(results);
  }
}
