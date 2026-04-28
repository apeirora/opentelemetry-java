/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

/**
 * Hard-error thrown by {@link AuditRecordBuilder#emit()} when the audit sink cannot be reached and
 * all retries are exhausted. This is an unchecked exception so that audit-logging call sites remain
 * clean, but callers SHOULD catch it and escalate the failure through their incident-management
 * process.
 */
public final class AuditDeliveryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AuditDeliveryException(String message) {
    super(message);
  }

  public AuditDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
