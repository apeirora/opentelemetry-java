/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

class DefaultAuditLogger implements AuditLogger {

  private static final AuditLogger INSTANCE = new DefaultAuditLogger();
  private static final AuditRecordBuilder NOOP_BUILDER = new NoopAuditRecordBuilder();
  private static final AuditReceipt NOOP_RECEIPT = AuditReceipt.create("", "", 0);

  private DefaultAuditLogger() {}

  static AuditLogger getInstance() {
    return INSTANCE;
  }

  @Override
  public AuditRecordBuilder auditRecordBuilder() {
    return NOOP_BUILDER;
  }

  private static final class NoopAuditRecordBuilder implements AuditRecordBuilder {

    private NoopAuditRecordBuilder() {}

    @Override
    public AuditRecordBuilder setRecordId(String recordId) {
      return this;
    }

    @Override
    public AuditRecordBuilder setTimestamp(long timestamp, TimeUnit unit) {
      return this;
    }

    @Override
    public AuditRecordBuilder setTimestamp(Instant instant) {
      return this;
    }

    @Override
    public AuditRecordBuilder setEventName(String eventName) {
      return this;
    }

    @Override
    public AuditRecordBuilder setActorId(String actorId) {
      return this;
    }

    @Override
    public AuditRecordBuilder setActorType(ActorType actorType) {
      return this;
    }

    @Override
    public AuditRecordBuilder setAction(String action) {
      return this;
    }

    @Override
    public AuditRecordBuilder setOutcome(Outcome outcome) {
      return this;
    }

    @Override
    public AuditRecordBuilder setObservedTimestamp(long timestamp, TimeUnit unit) {
      return this;
    }

    @Override
    public AuditRecordBuilder setObservedTimestamp(Instant instant) {
      return this;
    }

    @Override
    public AuditRecordBuilder setSchemaVersion(String schemaVersion) {
      return this;
    }

    @Override
    public AuditRecordBuilder setTargetId(String targetId) {
      return this;
    }

    @Override
    public AuditRecordBuilder setTargetType(String targetType) {
      return this;
    }

    @Override
    public AuditRecordBuilder setSourceId(String sourceId) {
      return this;
    }

    @Override
    public AuditRecordBuilder setSourceType(String sourceType) {
      return this;
    }

    @Override
    public AuditRecordBuilder setBody(Value<?> body) {
      return this;
    }

    @Override
    public <T> AuditRecordBuilder setAttribute(AttributeKey<T> key, @Nullable T value) {
      return this;
    }

    @Override
    public AuditRecordBuilder setSignature(byte[] signature, String algorithm) {
      return this;
    }

    @Override
    public AuditRecordBuilder setCertificate(byte[] certificate) {
      return this;
    }

    @Override
    public AuditRecordBuilder setHmac(byte[] hmac, String algorithm) {
      return this;
    }

    @Override
    public AuditRecordBuilder setSequenceNo(long sequenceNo) {
      return this;
    }

    @Override
    public AuditRecordBuilder setPrevHash(String prevHash) {
      return this;
    }

    @Override
    public AuditReceipt emit() {
      return NOOP_RECEIPT;
    }
  }
}
