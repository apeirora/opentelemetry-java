/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.AuditRecordBuilder;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.internal.AttributesMap;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** SDK implementation of {@link AuditRecordBuilder}. */
final class SdkAuditRecordBuilder implements AuditRecordBuilder {

  private final SdkAuditProvider provider;
  private final SdkAuditProvider.AuditLoggerKey loggerKey;

  // Required fields
  @Nullable private String recordId;
  private long timestampEpochNanos;
  @Nullable private String eventName;
  @Nullable private String actorId;
  @Nullable private ActorType actorType;
  @Nullable private String action;
  @Nullable private Outcome outcome;

  // Optional fields
  private long observedTimestampEpochNanos;
  @Nullable private String schemaVersion;
  @Nullable private String targetId;
  @Nullable private String targetType;
  @Nullable private String sourceId;
  @Nullable private String sourceType;
  @Nullable private Value<?> body;
  @Nullable private AttributesMap attributes;
  @Nullable private byte[] signature;
  @Nullable private String algorithm;
  @Nullable private byte[] certificate;
  @Nullable private byte[] hmac;
  @Nullable private String hmacAlgorithm;
  private long sequenceNo;
  @Nullable private String prevHash;

  SdkAuditRecordBuilder(SdkAuditProvider provider, SdkAuditProvider.AuditLoggerKey loggerKey) {
    this.provider = provider;
    this.loggerKey = loggerKey;
  }

  @Override
  public SdkAuditRecordBuilder setRecordId(String recordId) {
    this.recordId = recordId;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setTimestamp(long timestamp, TimeUnit unit) {
    this.timestampEpochNanos = unit.toNanos(timestamp);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setTimestamp(Instant instant) {
    this.timestampEpochNanos =
        TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setEventName(String eventName) {
    this.eventName = eventName;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setActorId(String actorId) {
    this.actorId = actorId;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setActorType(ActorType actorType) {
    this.actorType = actorType;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setAction(String action) {
    this.action = action;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setOutcome(Outcome outcome) {
    this.outcome = outcome;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setObservedTimestamp(long timestamp, TimeUnit unit) {
    this.observedTimestampEpochNanos = unit.toNanos(timestamp);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setObservedTimestamp(Instant instant) {
    this.observedTimestampEpochNanos =
        TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setTargetId(String targetId) {
    this.targetId = targetId;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setTargetType(String targetType) {
    this.targetType = targetType;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSourceId(String sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSourceType(String sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setBody(Value<?> body) {
    this.body = body;
    return this;
  }

  @Override
  public <T> SdkAuditRecordBuilder setAttribute(AttributeKey<T> key, @Nullable T value) {
    if (key == null || value == null) {
      return this;
    }
    if (attributes == null) {
      attributes = AttributesMap.create(128, Integer.MAX_VALUE);
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSignature(byte[] signature, String algorithm) {
    this.signature = signature;
    this.algorithm = algorithm;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setCertificate(byte[] certificate) {
    this.certificate = certificate;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setHmac(byte[] hmac, String algorithm) {
    this.hmac = hmac;
    this.hmacAlgorithm = algorithm;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSequenceNo(long sequenceNo) {
    this.sequenceNo = sequenceNo;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setPrevHash(String prevHash) {
    this.prevHash = prevHash;
    return this;
  }

  @Override
  public AuditReceipt emit() {
    if (provider.isShutdown()) {
      throw new AuditDeliveryException(
          "AuditProvider has been shut down; cannot emit audit records");
    }

    // Step 1: Generate RecordId if absent
    if (recordId == null || recordId.isEmpty()) {
      recordId = UUID.randomUUID().toString();
    }

    // Step 2: Set ObservedTimestamp if absent
    if (observedTimestampEpochNanos == 0) {
      observedTimestampEpochNanos = provider.getClock().now();
    }

    // Step 3: Validate required fields
    validateRequired("Timestamp", timestampEpochNanos != 0, "Timestamp must be set");
    validateRequired(
        "EventName",
        eventName != null && !eventName.isEmpty(),
        "EventName must be set and non-empty");
    validateRequired(
        "ActorId", actorId != null && !actorId.isEmpty(), "ActorId must be set and non-empty");
    validateRequired("ActorType", actorType != null, "ActorType must be set");
    validateRequired(
        "Action", action != null && !action.isEmpty(), "Action must be set and non-empty");
    validateRequired("Outcome", outcome != null, "Outcome must be set");

    String validatedEventName = Objects.requireNonNull(eventName);
    String validatedActorId = Objects.requireNonNull(actorId);
    ActorType validatedActorType = Objects.requireNonNull(actorType);
    String validatedAction = Objects.requireNonNull(action);
    Outcome validatedOutcome = Objects.requireNonNull(outcome);

    // Step 4+5: Create the mutable record and pass it through all processors.
    // Transfer ownership of the attributes map to the record (builder must not be reused).
    AttributesMap recordAttributes = this.attributes;
    this.attributes = null;
    SdkReadWriteAuditRecord rwRecord =
        new SdkReadWriteAuditRecord(
            provider.getResource(),
            loggerKey.getName(),
            loggerKey.getVersion(),
            loggerKey.getSchemaUrl(),
            recordId,
            timestampEpochNanos,
            observedTimestampEpochNanos,
            validatedEventName,
            validatedActorId,
            validatedActorType,
            validatedAction,
            validatedOutcome,
            targetId,
            targetType,
            sourceId,
            sourceType,
            body,
            recordAttributes,
            signature,
            algorithm,
            certificate,
            hmac,
            hmacAlgorithm,
            sequenceNo,
            prevHash,
            schemaVersion);

    provider.getProcessor().onEmit(Context.current(), rwRecord);

    // Step 6+7: Retrieve and return the receipt set by the exporter-wrapping processor
    AuditReceipt receipt = rwRecord.getReceipt();
    if (receipt == null) {
      throw new AuditDeliveryException(
          "Audit pipeline returned no receipt; ensure an AuditRecordExporter is configured");
    }
    return receipt;
  }

  private static void validateRequired(String field, boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(
          "AuditRecord validation failed for field '" + field + "': " + message);
    }
  }
}
