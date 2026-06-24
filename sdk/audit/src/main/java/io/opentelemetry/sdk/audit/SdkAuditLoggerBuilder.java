/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.AuditLogger;
import io.opentelemetry.api.audit.AuditLoggerBuilder;
import javax.annotation.Nullable;

/** SDK implementation of {@link AuditLoggerBuilder}. */
public class SdkAuditLoggerBuilder implements AuditLoggerBuilder {

  private final SdkAuditProvider provider;
  private final String name;
  @Nullable private String version;
  @Nullable private String schemaUrl;

  SdkAuditLoggerBuilder(SdkAuditProvider provider, String name) {
    this.provider = provider;
    this.name = name;
  }

  @Override
  public SdkAuditLoggerBuilder setSchemaUrl(String schemaUrl) {
    this.schemaUrl = schemaUrl;
    return this;
  }

  @Override
  public SdkAuditLoggerBuilder setInstrumentationVersion(String instrumentationVersion) {
    this.version = instrumentationVersion;
    return this;
  }

  @Override
  public AuditLogger build() {
    return provider.getOrCreateLogger(
        new SdkAuditProvider.AuditLoggerKey(name, version, schemaUrl));
  }
}
