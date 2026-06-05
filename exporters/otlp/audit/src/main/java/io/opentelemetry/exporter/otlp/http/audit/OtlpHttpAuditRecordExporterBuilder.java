/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.http.audit;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.exporter.otlp.internal.HttpExporterBuilder;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/**
 * Builder for {@link OtlpHttpAuditRecordExporter}.
 *
 * <p>Defaults: endpoint {@code http://localhost:4318/v1/audit}, 10 s timeout.
 */
public final class OtlpHttpAuditRecordExporterBuilder {

  private final HttpExporterBuilder delegate;

  OtlpHttpAuditRecordExporterBuilder() {
    this.delegate =
        new HttpExporterBuilder(
            StandardComponentId.ExporterType.OTLP_HTTP_LOG_EXPORTER,
            OtlpHttpAuditRecordExporter.DEFAULT_ENDPOINT);
  }

  OtlpHttpAuditRecordExporterBuilder(HttpExporterBuilder delegate) {
    this.delegate = delegate;
  }

  /** Sets the maximum time to wait for the collector to process an exported batch. */
  public OtlpHttpAuditRecordExporterBuilder setTimeout(long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    return setTimeout(Duration.ofNanos(unit.toNanos(timeout)));
  }

  /** Sets the maximum time to wait for the collector to process an exported batch. */
  public OtlpHttpAuditRecordExporterBuilder setTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    delegate.setTimeout(timeout);
    return this;
  }

  /** Sets the OTLP endpoint URL. Defaults to {@code http://localhost:4318/v1/audit}. */
  public OtlpHttpAuditRecordExporterBuilder setEndpoint(String endpoint) {
    requireNonNull(endpoint, "endpoint");
    delegate.setEndpoint(endpoint);
    return this;
  }

  /** Adds a constant HTTP header sent with every request. */
  public OtlpHttpAuditRecordExporterBuilder addHeader(String key, String value) {
    delegate.addConstantHeaders(key, value);
    return this;
  }

  /** Adds constant HTTP headers sent with every request. */
  public OtlpHttpAuditRecordExporterBuilder setHeaders(Map<String, String> headers) {
    headers.forEach(delegate::addConstantHeaders);
    return this;
  }

  /** Configures TLS for the OTLP connection. */
  public OtlpHttpAuditRecordExporterBuilder setSslContext(
      SSLContext sslContext, X509TrustManager trustManager) {
    requireNonNull(sslContext, "sslContext");
    requireNonNull(trustManager, "trustManager");
    delegate.setSslContext(sslContext, trustManager);
    return this;
  }

  /**
   * Sets the retry policy. Audit records MUST NOT be silently dropped on retry exhaustion; the
   * exporter will surface a hard error instead.
   */
  public OtlpHttpAuditRecordExporterBuilder setRetryPolicy(@Nullable RetryPolicy retryPolicy) {
    delegate.setRetryPolicy(retryPolicy);
    return this;
  }

  /** Builds and returns the configured {@link OtlpHttpAuditRecordExporter}. */
  public OtlpHttpAuditRecordExporter build() {
    return new OtlpHttpAuditRecordExporter(delegate, delegate.build());
  }
}
