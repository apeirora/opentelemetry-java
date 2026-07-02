/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditLoggerBuilder;
import io.opentelemetry.api.audit.AuditProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * SDK implementation of {@link AuditProvider}.
 *
 * <p>Create via {@link #builder()}. The provider maintains a dedicated queue and exporter pipeline
 * that is completely independent of the Log signal pipeline.
 *
 * <p>The provider intentionally does NOT expose sampler configuration. Any attempt to configure
 * sampling on the audit pipeline is a configuration error and will be rejected.
 */
public class SdkAuditProvider implements AuditProvider, Closeable {

  private static final Logger logger = Logger.getLogger(SdkAuditProvider.class.getName());

  private final Resource resource;
  private final AuditRecordProcessor processor;
  private final Clock clock;
  private final ConcurrentHashMap<AuditLoggerKey, SdkAuditLogger> loggerRegistry =
      new ConcurrentHashMap<>();
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  SdkAuditProvider(Resource resource, List<AuditRecordProcessor> processors, Clock clock) {
    this.resource = resource;
    this.processor = AuditRecordProcessor.composite(processors);
    this.clock = clock;
  }

  /** Returns a new {@link SdkAuditProviderBuilder}. */
  public static SdkAuditProviderBuilder builder() {
    return new SdkAuditProviderBuilder();
  }

  @Override
  public AuditLoggerBuilder auditLoggerBuilder(String name) {
    if (isShutdown.get()) {
      throw new AuditDeliveryException(
          "AuditProvider has been shut down; cannot create new AuditLoggers");
    }
    if (name.isEmpty()) {
      logger.log(
          Level.WARNING,
          "AuditProvider.auditLoggerBuilder() called with empty name; using 'unknown'");
      return new SdkAuditLoggerBuilder(this, "unknown");
    }
    return new SdkAuditLoggerBuilder(this, name);
  }

  /** Returns the {@link SdkAuditLogger} for the given key, creating it if necessary. */
  SdkAuditLogger getOrCreateLogger(AuditLoggerKey key) {
    return loggerRegistry.computeIfAbsent(key, k -> new SdkAuditLogger(this, k));
  }

  Resource getResource() {
    return resource;
  }

  AuditRecordProcessor getProcessor() {
    return processor;
  }

  Clock getClock() {
    return clock;
  }

  boolean isShutdown() {
    return isShutdown.get();
  }

  /**
   * Shuts down this provider. Calls {@link #forceFlush()} then shuts down all registered
   * processors.
   *
   * @return a result indicating whether shutdown succeeded
   */
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      return CompletableResultCode.ofSuccess();
    }
    CompletableResultCode result = new CompletableResultCode();
    CompletableResultCode flushResult = forceFlush();
    flushResult.whenComplete(
        () -> {
          CompletableResultCode shutdownResult = processor.shutdown();
          shutdownResult.whenComplete(
              () -> {
                if (!flushResult.isSuccess() || !shutdownResult.isSuccess()) {
                  result.fail();
                } else {
                  result.succeed();
                }
              });
        });
    return result;
  }

  /**
   * Forces all buffered audit records to be exported.
   *
   * @return a result indicating whether the flush succeeded
   */
  public CompletableResultCode forceFlush() {
    if (isShutdown.get()) {
      return CompletableResultCode.ofSuccess();
    }
    return processor.forceFlush();
  }

  @Override
  public void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }

  // ── Inner key type ────────────────────────────────────────────────────────

  static final class AuditLoggerKey {

    private final String name;
    @Nullable private final String version;
    @Nullable private final String schemaUrl;

    AuditLoggerKey(String name, @Nullable String version, @Nullable String schemaUrl) {
      this.name = name;
      this.version = version;
      this.schemaUrl = schemaUrl;
    }

    String getName() {
      return name;
    }

    @Nullable
    String getVersion() {
      return version;
    }

    @Nullable
    String getSchemaUrl() {
      return schemaUrl;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof AuditLoggerKey)) {
        return false;
      }
      AuditLoggerKey other = (AuditLoggerKey) obj;
      return name.equals(other.name)
          && Objects.equals(version, other.version)
          && Objects.equals(schemaUrl, other.schemaUrl);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version, schemaUrl);
    }
  }
}
