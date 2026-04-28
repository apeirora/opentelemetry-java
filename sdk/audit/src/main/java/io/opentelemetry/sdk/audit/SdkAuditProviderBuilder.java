/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;

/** Builder for {@link SdkAuditProvider}. */
public final class SdkAuditProviderBuilder {

  private Resource resource = Resource.getDefault();
  private Clock clock = Clock.getDefault();
  private final List<AuditRecordProcessor> processors = new ArrayList<>();

  SdkAuditProviderBuilder() {}

  /**
   * Sets the {@link Resource} to be associated with all audit records emitted by this provider.
   */
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
   * Adds an {@link AuditRecordProcessor} to the pipeline. Processors are invoked in the order
   * they are added.
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

  /** Builds and returns the configured {@link SdkAuditProvider}. */
  public SdkAuditProvider build() {
    return new SdkAuditProvider(resource, processors, clock);
  }
}
