/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.internal.GuardedBy;
import io.opentelemetry.sdk.common.internal.AttributesMap;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Mutable view of an audit record passed to {@link AuditRecordProcessor#onEmit}. Processors MAY
 * enrich the record by adding attributes but MUST NOT modify the mandatory fields.
 */
@ThreadSafe
final class SdkReadWriteAuditRecord implements ReadWriteAuditRecord {

  private final Resource resource;
  private final String loggerName;
  @Nullable private final String loggerVersion;
  @Nullable private final String schemaUrl;
  private final String recordId;
  private final long timestampEpochNanos;
  private final long observedTimestampEpochNanos;
  private final String eventName;
  private final String actorId;
  private final ActorType actorType;
  private final String action;
  private final Outcome outcome;
  @Nullable private final String targetId;
  @Nullable private final String targetType;
  @Nullable private final String sourceId;
  @Nullable private final String sourceType;
  @Nullable private final Value<?> body;
  @Nullable private final byte[] integrityValue;
  private final long sequenceNo;
  @Nullable private final String prevHash;
  @Nullable private final String schemaVersion;

  private final Object lock = new Object();

  @GuardedBy("lock")
  @Nullable
  private AttributesMap attributes;

  @GuardedBy("lock")
  @Nullable
  private AuditReceipt receipt;

  @SuppressWarnings("TooManyParameters")
  SdkReadWriteAuditRecord(
      Resource resource,
      String loggerName,
      @Nullable String loggerVersion,
      @Nullable String schemaUrl,
      String recordId,
      long timestampEpochNanos,
      long observedTimestampEpochNanos,
      String eventName,
      String actorId,
      ActorType actorType,
      String action,
      Outcome outcome,
      @Nullable String targetId,
      @Nullable String targetType,
      @Nullable String sourceId,
      @Nullable String sourceType,
      @Nullable Value<?> body,
      @Nullable AttributesMap attributes,
      @Nullable byte[] integrityValue,
      long sequenceNo,
      @Nullable String prevHash,
      @Nullable String schemaVersion) {
    this.resource = resource;
    this.loggerName = loggerName;
    this.loggerVersion = loggerVersion;
    this.schemaUrl = schemaUrl;
    this.recordId = recordId;
    this.timestampEpochNanos = timestampEpochNanos;
    this.observedTimestampEpochNanos = observedTimestampEpochNanos;
    this.eventName = eventName;
    this.actorId = actorId;
    this.actorType = actorType;
    this.action = action;
    this.outcome = outcome;
    this.targetId = targetId;
    this.targetType = targetType;
    this.sourceId = sourceId;
    this.sourceType = sourceType;
    this.body = body;
    this.attributes = attributes;
    this.integrityValue = integrityValue;
    this.sequenceNo = sequenceNo;
    this.prevHash = prevHash;
    this.schemaVersion = schemaVersion;
  }

  @Override
  public <T> ReadWriteAuditRecord setAttribute(AttributeKey<T> key, @Nullable T value) {
    if (key == null || value == null) {
      return this;
    }
    synchronized (lock) {
      if (attributes == null) {
        attributes = AttributesMap.create(128, Integer.MAX_VALUE);
      }
      attributes.put(key, value);
    }
    return this;
  }

  @Override
  public void setReceipt(AuditReceipt receipt) {
    synchronized (lock) {
      this.receipt = receipt;
    }
  }

  @Override
  @Nullable
  public AuditReceipt getReceipt() {
    synchronized (lock) {
      return receipt;
    }
  }

  @Override
  public AuditRecordData toAuditRecordData() {
    Attributes frozenAttributes;
    synchronized (lock) {
      frozenAttributes = attributes != null ? attributes.immutableCopy() : Attributes.empty();
    }
    return SdkAuditRecordData.create(
        resource,
        loggerName,
        loggerVersion,
        schemaUrl,
        recordId,
        timestampEpochNanos,
        observedTimestampEpochNanos,
        eventName,
        actorId,
        actorType,
        action,
        outcome,
        targetId,
        targetType,
        sourceId,
        sourceType,
        body,
        frozenAttributes,
        integrityValue,
        sequenceNo,
        prevHash,
        schemaVersion);
  }

  // ── Read accessors for processors ─────────────────────────────────────────

  @Override
  public String getRecordId() {
    return recordId;
  }

  @Override
  public long getTimestampEpochNanos() {
    return timestampEpochNanos;
  }

  @Override
  public String getEventName() {
    return eventName;
  }

  @Override
  public String getActorId() {
    return actorId;
  }

  @Override
  public ActorType getActorType() {
    return actorType;
  }

  @Override
  public String getAction() {
    return action;
  }

  @Override
  public Outcome getOutcome() {
    return outcome;
  }
}
