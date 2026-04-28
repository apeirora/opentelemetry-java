/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import javax.annotation.concurrent.ThreadSafe;

/**
 * The entry point for emitting audit records.
 *
 * <p>Obtain an {@link AuditRecordBuilder} via {@link #auditRecordBuilder()}, populate all required
 * fields, and call {@link AuditRecordBuilder#emit()} to deliver the record to the audit sink.
 *
 * <p>Unlike {@link io.opentelemetry.api.logs.Logger}, this interface does <em>not</em> expose an
 * {@code isEnabled} check: audit records are ALWAYS emitted. Dropping audit records is prohibited
 * by the audit logging specification.
 */
@ThreadSafe
public interface AuditLogger {

  /** Returns an {@link AuditRecordBuilder} for constructing and emitting an audit record. */
  AuditRecordBuilder auditRecordBuilder();
}
