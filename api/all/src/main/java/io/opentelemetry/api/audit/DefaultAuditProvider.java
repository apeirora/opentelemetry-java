/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

class DefaultAuditProvider implements AuditProvider {

  private static final AuditProvider INSTANCE = new DefaultAuditProvider();
  private static final AuditLoggerBuilder NOOP_BUILDER = new NoopAuditLoggerBuilder();

  private DefaultAuditProvider() {}

  static AuditProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public AuditLoggerBuilder auditLoggerBuilder(String name) {
    return NOOP_BUILDER;
  }

  private static class NoopAuditLoggerBuilder implements AuditLoggerBuilder {

    @Override
    public AuditLoggerBuilder setSchemaUrl(String schemaUrl) {
      return this;
    }

    @Override
    public AuditLoggerBuilder setInstrumentationVersion(String instrumentationVersion) {
      return this;
    }

    @Override
    public AuditLogger build() {
      return DefaultAuditLogger.getInstance();
    }
  }
}
