/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;

/** No-op {@link AuditRecordProcessor} returned when no processors are registered. */
final class NoopAuditRecordProcessor implements AuditRecordProcessor {

  private static final AuditRecordProcessor INSTANCE = new NoopAuditRecordProcessor();

  private NoopAuditRecordProcessor() {}

  static AuditRecordProcessor getInstance() {
    return INSTANCE;
  }

  @Override
  public void onEmit(Context context, ReadWriteAuditRecord record) {}

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }
}
