/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.autoconfigure.spi.audit;

import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * A service provider interface (SPI) for providing audit record exporters that can be used with
 * the autoconfigured SDK. If the {@code otel.audit.exporter} property contains a value equal to
 * what is returned by {@link #getName()}, the exporter returned by {@link
 * #createExporter(ConfigProperties)} will be enabled and added to the audit pipeline.
 *
 * <p>This SPI is at {@code Development} stability; the interface may change in future releases.
 */
public interface ConfigurableAuditRecordExporterProvider {

  /**
   * Returns an {@link AuditRecordExporter} that can be registered to the audit pipeline by
   * providing the property value specified by {@link #getName()}.
   */
  AuditRecordExporter createExporter(ConfigProperties config);

  /**
   * Returns the name of this exporter, which can be specified with the {@code otel.audit.exporter}
   * property to enable it. The name returned MUST NOT be the same as any other exporter name.
   */
  String getName();
}
