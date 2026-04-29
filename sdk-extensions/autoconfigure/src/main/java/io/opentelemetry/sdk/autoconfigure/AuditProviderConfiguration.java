/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.api.audit.GlobalAuditProvider;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.audit.AuditRecordProcessor;
import io.opentelemetry.sdk.audit.SdkAuditProvider;
import io.opentelemetry.sdk.audit.SdkAuditProviderBuilder;
import io.opentelemetry.sdk.audit.export.SimpleAuditRecordProcessor;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Configures an {@link SdkAuditProvider} from autoconfiguration properties and registers it as the
 * global {@link io.opentelemetry.api.audit.AuditProvider}.
 *
 * <p>The property {@code otel.audit.exporter} controls which exporter is used (default: {@code
 * otlp}). Use {@code otel.audit.exporter=none} to disable audit logging.
 */
final class AuditProviderConfiguration {

  static SdkAuditProvider configureAuditProvider(
      Resource resource, ConfigProperties config, SpiHelper spiHelper, List<Closeable> closeables) {

    Map<String, AuditRecordExporter> exportersByName =
        AuditExporterConfiguration.configureAuditRecordExporters(config, spiHelper, closeables);

    SdkAuditProviderBuilder builder = SdkAuditProvider.builder().setResource(resource);

    // Each configured exporter gets its own SimpleAuditRecordProcessor in the chain.
    for (AuditRecordExporter exporter : exportersByName.values()) {
      AuditRecordProcessor processor = SimpleAuditRecordProcessor.create(exporter);
      closeables.add(processor);
      builder.addAuditRecordProcessor(processor);
    }

    SdkAuditProvider provider = builder.build();
    closeables.add(provider);
    GlobalAuditProvider.set(provider);
    return provider;
  }

  private AuditProviderConfiguration() {}
}
