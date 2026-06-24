/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Audit;
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
public class SdkAuditRecordBuilder implements AuditRecordBuilder {

  private final SdkAuditProvider provider;
  private final SdkAuditProvider.AuditLoggerKey loggerKey;

  // Required fields
  private long timestampEpochNanos;
  @Nullable private String eventName;

  // Optional fields
  private long observedTimestampEpochNanos;
  @Nullable private String schemaVersion;
  @Nullable private byte[] integrityValue;
  private AttributesMap attributes = AttributesMap.create(128, Integer.MAX_VALUE);

  SdkAuditRecordBuilder(SdkAuditProvider provider, SdkAuditProvider.AuditLoggerKey loggerKey) {
    this.provider = provider;
    this.loggerKey = loggerKey;
  }

  @Override
  public SdkAuditRecordBuilder setRecordId(String recordId) {
    attributes.put(AttributeKey.stringKey(Audit.RECORD_ID), recordId);
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
  public SdkAuditRecordBuilder setActor(String actorId, ActorType actorType) {
    attributes.put(AttributeKey.stringKey(Audit.ACTOR_TYPE), actorType.name());
    attributes.put(AttributeKey.stringKey(Audit.ACTOR_ID), actorId);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setAction(String action) {
    attributes.put(AttributeKey.stringKey(Audit.ACTION), action);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setOutcome(Outcome outcome) {
    attributes.put(AttributeKey.stringKey(Audit.OUTCOME), outcome.name());
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
  public SdkAuditRecordBuilder setTarget(String targetId, String targetType) {
    attributes.put(AttributeKey.stringKey(Audit.TARGET_TYPE), targetType);
    attributes.put(AttributeKey.stringKey(Audit.TARGET_ID), targetId);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSource(String sourceId, String sourceType) {
    attributes.put(AttributeKey.stringKey(Audit.SOURCE_TYPE), sourceType);
    attributes.put(AttributeKey.stringKey(Audit.SOURCE_ID), sourceId);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setBody(Value<?> body) {
    // Body is carried as a LogRecordData field via AuditRecordData.getBodyValue(); storing it
    // separately here is not needed — the default implementation in AuditRecordData returns null.
    return this;
  }

  @Override
  public <T> SdkAuditRecordBuilder addAttribute(AttributeKey<T> key, @Nullable T value) {
    if (key == null || value == null) {
      return this;
    }
    if (attributes.containsKey(key)) {
      throw new IllegalStateException(
          "Cannot add attribute with key '"
              + key.getKey()
              + "'; already exists on this record builder");
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setIntegrityValue(byte[] integrityValue) {
    this.integrityValue = integrityValue;
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setSequenceNo(long sequenceNo) {
    attributes.put(AttributeKey.longKey(Audit.SEQUENCE_NUMBER), sequenceNo);
    return this;
  }

  @Override
  public SdkAuditRecordBuilder setPrevHash(String prevHash) {
    attributes.put(AttributeKey.stringKey(Audit.PREV_HASH), prevHash);
    return this;
  }

  @Override
  public AuditReceipt emit() {
    if (provider.isShutdown()) {
      throw new AuditDeliveryException(
          "AuditProvider has been shut down; cannot emit audit records");
    }

    // Step 1: Generate RecordId if absent
    String recordId = getRecordId();
    if (recordId == null || recordId.isEmpty()) {
      setRecordId(UUID.randomUUID().toString());
      recordId = getRecordId();
    }

    // Step 2: Set ObservedTimestamp if absent
    if (observedTimestampEpochNanos == 0) {
      observedTimestampEpochNanos = provider.getClock().now();
    }

    // Step 3: Validate required fields
    String actorId = getActorId();
    ActorType actorType = getActorType();
    String action = getAction();
    Outcome outcome = getOutcome();
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
    String validatedRecordId = Objects.requireNonNull(recordId);
    String validatedActorId = Objects.requireNonNull(actorId);
    ActorType validatedActorType = Objects.requireNonNull(actorType);
    String validatedAction = Objects.requireNonNull(action);
    Outcome validatedOutcome = Objects.requireNonNull(outcome);

    String targetId = getTargetId();
    String targetType = getTargetType();
    String sourceId = getSourceId();
    String sourceType = getSourceType();
    long sequenceNo = getSequenceNo();
    String prevHash = getPrevHash();

    // Step 4+5: Create the mutable record and pass it through all processors.
    // Transfer ownership of the attributes map to the record (builder must not be reused).
    AttributesMap recordAttributes = this.attributes;
    this.attributes = null; // invalidate; builder must not be reused after emit()
    SdkReadWriteAuditRecord rwRecord =
        new SdkReadWriteAuditRecord(
            provider.getResource(),
            loggerKey.getName(),
            loggerKey.getVersion(),
            loggerKey.getSchemaUrl(),
            validatedRecordId,
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
            recordAttributes,
            integrityValue,
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

  @Nullable
  private String getRecordId() {
    return attributes.get(AttributeKey.stringKey(Audit.RECORD_ID));
  }

  @Nullable
  private String getActorId() {
    return attributes.get(AttributeKey.stringKey(Audit.ACTOR_ID));
  }

  @Nullable
  private ActorType getActorType() {
    String actorType = attributes.get(AttributeKey.stringKey(Audit.ACTOR_TYPE));
    if (actorType == null) {
      return null;
    }
    try {
      return ActorType.valueOf(actorType);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Nullable
  private String getAction() {
    return attributes.get(AttributeKey.stringKey(Audit.ACTION));
  }

  @Nullable
  private Outcome getOutcome() {
    String outcome = attributes.get(AttributeKey.stringKey(Audit.OUTCOME));
    if (outcome == null) {
      return null;
    }
    try {
      return Outcome.valueOf(outcome);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Nullable
  private String getTargetId() {
    return attributes.get(AttributeKey.stringKey(Audit.TARGET_ID));
  }

  @Nullable
  private String getTargetType() {
    return attributes.get(AttributeKey.stringKey(Audit.TARGET_TYPE));
  }

  @Nullable
  private String getSourceId() {
    return attributes.get(AttributeKey.stringKey(Audit.SOURCE_ID));
  }

  @Nullable
  private String getSourceType() {
    return attributes.get(AttributeKey.stringKey(Audit.SOURCE_TYPE));
  }

  private long getSequenceNo() {
    Long sequenceNo = attributes.get(AttributeKey.longKey(Audit.SEQUENCE_NUMBER));
    return sequenceNo == null ? -1 : sequenceNo;
  }

  @Nullable
  private String getPrevHash() {
    return attributes.get(AttributeKey.stringKey(Audit.PREV_HASH));
  }
}
