/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Builder for {@link SdkAuditProvider}. */
public class SdkAuditProviderBuilder {

  private static final AttributeKey<String> ATTR_INTEGRITY_ALGORITHM =
      AttributeKey.stringKey("audit.integrity.algorithm");
  private static final AttributeKey<String> ATTR_INTEGRITY_CERTIFICATE =
      AttributeKey.stringKey("audit.integrity.certificate");

  private Resource resource = Resource.getDefault();
  private Clock clock = Clock.getDefault();
  private final List<AuditRecordProcessor> processors = new ArrayList<>();
  @Nullable private String integrityAlgorithm;
  @Nullable private String integrityCertificate;

  SdkAuditProviderBuilder() {}

  /** Sets the {@link Resource} to be associated with all audit records emitted by this provider. */
  public SdkAuditProviderBuilder setResource(Resource resource) {
    if (resource == null) {
      throw new NullPointerException("resource");
    }
    this.resource = resource;
    return this;
  }

  /** Sets the {@link Clock} used for {@code ObservedTimestamp} generation. */
  public SdkAuditProviderBuilder setClock(Clock clock) {
    if (clock == null) {
      throw new NullPointerException("clock");
    }
    this.clock = clock;
    return this;
  }

  /**
   * Adds an {@link AuditRecordProcessor} to the pipeline. Processors are invoked in the order they
   * are added.
   *
   * <p>The last processor in the chain is responsible for forwarding records to the exporter and
   * setting the {@link io.opentelemetry.api.audit.AuditReceipt} on the record.
   */
  public SdkAuditProviderBuilder addAuditRecordProcessor(AuditRecordProcessor processor) {
    if (processor == null) {
      throw new NullPointerException("processor");
    }
    processors.add(processor);
    return this;
  }

  /**
   * Sets the {@code audit.integrity.algorithm} Resource attribute. MUST be set when any record
   * emitted by this provider carries an {@code audit.integrity.value} (i.e. when {@link
   * io.opentelemetry.api.audit.AuditRecordBuilder#setIntegrityValue(byte[])} is used).
   *
   * <p>For asymmetric signatures use a JWA algorithm identifier (e.g. {@code "ES256"}, {@code
   * "RS256"}, {@code "EdDSA"}). For HMACs use an IANA MAC algorithm identifier (e.g. {@code
   * "HMAC-SHA256"}).
   */
  public SdkAuditProviderBuilder setIntegrityAlgorithm(String algorithm) {
    if (algorithm == null) {
      throw new NullPointerException("algorithm");
    }
    this.integrityAlgorithm = algorithm;
    return this;
  }

  /**
   * Sets the {@code audit.integrity.certificate} Resource attribute. MUST NOT be set for HMAC
   * algorithms.
   *
   * <p>The value MUST be one of: base64-encoded DER certificate, fingerprint ({@code sha256:<hex>}
   * or {@code sha1:<hex>}), JWK Key ID, Subject Key Identifier (colon-separated hex), or
   * Issuer+Serial ({@code CN=...,O=.../serial}).
   */
  public SdkAuditProviderBuilder setIntegrityCertificate(String certificate) {
    if (certificate == null) {
      throw new NullPointerException("certificate");
    }
    this.integrityCertificate = certificate;
    return this;
  }

  /** Builds and returns the configured {@link SdkAuditProvider}. */
  public SdkAuditProvider build() {
    Resource effectiveResource = resource;
    if (integrityAlgorithm != null || integrityCertificate != null) {
      AttributesBuilder integrityAttrs = Attributes.builder();
      if (integrityAlgorithm != null) {
        integrityAttrs.put(ATTR_INTEGRITY_ALGORITHM, integrityAlgorithm);
      }
      if (integrityCertificate != null) {
        integrityAttrs.put(ATTR_INTEGRITY_CERTIFICATE, integrityCertificate);
      }
      effectiveResource = resource.merge(Resource.create(integrityAttrs.build()));
    }
    return new SdkAuditProvider(effectiveResource, processors, clock);
  }
}
