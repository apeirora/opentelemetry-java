/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import javax.annotation.concurrent.ThreadSafe;

/**
 * The entry point of the Audit Logging API. Provides named {@link AuditLogger} instances.
 *
 * <p>The provider is expected to be accessed from a central place. Use {@link
 * GlobalAuditProvider#get()} to obtain the globally registered instance, or create an {@link
 * AuditProvider} directly via the SDK.
 */
@ThreadSafe
public interface AuditProvider {

  /**
   * Gets or creates a named {@link AuditLogger} instance.
   *
   * @param name A string identifying the component or subsystem emitting audit records (for example
   *     {@code "com.example.auth"}). MUST NOT be empty.
   */
  default AuditLogger get(String name) {
    return auditLoggerBuilder(name).build();
  }

  /**
   * Creates an {@link AuditLoggerBuilder} for a named {@link AuditLogger}.
   *
   * @param name A string identifying the component or subsystem emitting audit records. MUST NOT be
   *     empty.
   */
  AuditLoggerBuilder auditLoggerBuilder(String name);
}
