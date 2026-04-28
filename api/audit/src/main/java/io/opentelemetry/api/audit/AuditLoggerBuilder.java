/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

/**
 * Builder for creating named {@link AuditLogger} instances.
 *
 * <p>The {@code name} (set on the owning {@link AuditProvider}) is stored as a diagnostic label on
 * the logger. Unlike {@link io.opentelemetry.api.logs.LoggerBuilder}, the name is NOT mapped to an
 * OTLP {@code InstrumentationScope}.
 */
public interface AuditLoggerBuilder {

  /**
   * Sets the schema URL to be recorded on emitted {@link AuditRecordBuilder}s for semantic
   * convention versioning.
   */
  AuditLoggerBuilder setSchemaUrl(String schemaUrl);

  /** Sets the version of the component or library that is emitting audit records. */
  AuditLoggerBuilder setInstrumentationVersion(String instrumentationVersion);

  /** Returns the configured {@link AuditLogger}. */
  AuditLogger build();
}
