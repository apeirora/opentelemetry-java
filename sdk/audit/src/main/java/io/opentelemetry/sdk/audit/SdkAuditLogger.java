/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.AuditLogger;
import io.opentelemetry.api.audit.AuditRecordBuilder;
import javax.annotation.concurrent.ThreadSafe;

/** SDK implementation of {@link AuditLogger}. */
@ThreadSafe
public class SdkAuditLogger implements AuditLogger {

  private final SdkAuditProvider provider;
  private final SdkAuditProvider.AuditLoggerKey key;

  SdkAuditLogger(SdkAuditProvider provider, SdkAuditProvider.AuditLoggerKey key) {
    this.provider = provider;
    this.key = key;
  }

  @Override
  public AuditRecordBuilder auditRecordBuilder() {
    return new SdkAuditRecordBuilder(provider, key);
  }
}
