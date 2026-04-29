/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.autoconfigure.internal.NamedSpiManager;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.audit.ConfigurableAuditRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AuditExporterConfiguration {

  private static final String EXPORTER_NONE = "none";

  static Map<String, AuditRecordExporter> configureAuditRecordExporters(
      ConfigProperties config, SpiHelper spiHelper, List<Closeable> closeables) {
    Set<String> exporterNames = DefaultConfigProperties.getSet(config, "otel.audit.exporter");

    if (exporterNames.contains(EXPORTER_NONE)) {
      if (exporterNames.size() > 1) {
        throw new ConfigurationException(
            "otel.audit.exporter contains " + EXPORTER_NONE + " along with other exporters");
      }
      return Collections.emptyMap();
    }

    if (exporterNames.isEmpty()) {
      exporterNames = Collections.singleton("otlp");
    }

    NamedSpiManager<AuditRecordExporter> spiManager = auditExporterSpiManager(config, spiHelper);

    Map<String, AuditRecordExporter> map = new HashMap<>();
    for (String name : exporterNames) {
      AuditRecordExporter exporter = spiManager.getByName(name);
      if (exporter == null) {
        throw new ConfigurationException("Unrecognized value for otel.audit.exporter: " + name);
      }
      closeables.add(exporter);
      map.put(name, exporter);
    }
    return Collections.unmodifiableMap(map);
  }

  static NamedSpiManager<AuditRecordExporter> auditExporterSpiManager(
      ConfigProperties config, SpiHelper spiHelper) {
    return spiHelper.loadConfigurable(
        ConfigurableAuditRecordExporterProvider.class,
        ConfigurableAuditRecordExporterProvider::getName,
        ConfigurableAuditRecordExporterProvider::createExporter,
        config);
  }

  private AuditExporterConfiguration() {}
}
