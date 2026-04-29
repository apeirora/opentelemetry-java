/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.http.audit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Base64;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Adapts an {@link AuditRecordData} to the {@link LogRecordData} interface so that the existing
 * OTLP log marshaling infrastructure can serialize audit records to the {@code
 * ExportLogsServiceRequest} protobuf message.
 *
 * <p>Mappings per the Audit Logging specification:
 *
 * <ul>
 *   <li>{@code SeverityNumber} MUST remain unset ({@code null}).
 *   <li>{@code InstrumentationScope} MUST be empty.
 *   <li>Mandatory audit fields are stored as {@code Attributes} with {@code audit.*} keys.
 * </ul>
 */
final class AuditLogRecordDataAdapter implements LogRecordData {

  // Mandatory attributes – spec section: Audit Semantic Attributes
  private static final String ATTR_RECORD_ID = "audit.record.id";
  private static final String ATTR_ACTOR_ID = "audit.actor.id";
  private static final String ATTR_ACTOR_TYPE = "audit.actor.type";
  private static final String ATTR_ACTION = "audit.action";
  private static final String ATTR_OUTCOME = "audit.outcome";

  // Optional attributes
  private static final String ATTR_TARGET_ID = "audit.target.id";
  private static final String ATTR_TARGET_TYPE = "audit.target.type";
  private static final String ATTR_SOURCE_ID = "audit.source.id";
  private static final String ATTR_SOURCE_TYPE = "audit.source.type";
  private static final String ATTR_INTEGRITY_VALUE = "audit.integrity.value";
  private static final String ATTR_SEQUENCE_NUMBER = "audit.sequence.number";
  private static final String ATTR_PREV_HASH = "audit.prev.hash";
  private static final String ATTR_SCHEMA_VERSION = "audit.schema.version";

  private final AuditRecordData audit;
  private final Attributes mergedAttributes;

  AuditLogRecordDataAdapter(AuditRecordData audit) {
    this.audit = audit;
    this.mergedAttributes = buildAttributes(audit);
  }

  private static Attributes buildAttributes(AuditRecordData a) {
    AttributesBuilder b = Attributes.builder();

    // Mandatory audit fields – spec-defined attribute keys and lowercase values
    b.put(AttributeKey.stringKey(ATTR_RECORD_ID), a.getRecordId());
    b.put(AttributeKey.stringKey(ATTR_ACTOR_ID), a.getActorId());
    b.put(
        AttributeKey.stringKey(ATTR_ACTOR_TYPE), a.getActorType().name().toLowerCase(Locale.ROOT));
    b.put(AttributeKey.stringKey(ATTR_ACTION), a.getAction());
    b.put(AttributeKey.stringKey(ATTR_OUTCOME), a.getOutcome().name().toLowerCase(Locale.ROOT));

    // Optional target attributes
    if (a.getTargetId() != null) {
      b.put(AttributeKey.stringKey(ATTR_TARGET_ID), a.getTargetId());
    }
    if (a.getTargetType() != null) {
      b.put(AttributeKey.stringKey(ATTR_TARGET_TYPE), a.getTargetType());
    }

    // Optional source attributes
    if (a.getSourceId() != null) {
      b.put(AttributeKey.stringKey(ATTR_SOURCE_ID), a.getSourceId());
    }
    if (a.getSourceType() != null) {
      b.put(AttributeKey.stringKey(ATTR_SOURCE_TYPE), a.getSourceType());
    }

    // Integrity value: base64-encode signature or HMAC into audit.integrity.value
    byte[] integrityBytes = a.getSignature() != null ? a.getSignature() : a.getHmac();
    if (integrityBytes != null) {
      b.put(
          AttributeKey.stringKey(ATTR_INTEGRITY_VALUE),
          Base64.getEncoder().encodeToString(integrityBytes));
    }

    // Ordering attributes
    if (a.getSequenceNo() != 0) {
      b.put(AttributeKey.longKey(ATTR_SEQUENCE_NUMBER), a.getSequenceNo());
    }
    if (a.getPrevHash() != null) {
      b.put(AttributeKey.stringKey(ATTR_PREV_HASH), a.getPrevHash());
    }

    // Schema version
    if (a.getSchemaVersion() != null) {
      b.put(AttributeKey.stringKey(ATTR_SCHEMA_VERSION), a.getSchemaVersion());
    }

    // User-supplied attributes (merged last so they can override if needed)
    a.getAttributes()
        .forEach(
            (key, value) -> {
              @SuppressWarnings("unchecked")
              AttributeKey<Object> castKey = (AttributeKey<Object>) key;
              b.put(castKey, value);
            });

    return b.build();
  }

  @Override
  public Resource getResource() {
    return audit.getResource();
  }

  /** Audit records do not use instrumentation scope; always returns empty. */
  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return InstrumentationScopeInfo.empty();
  }

  @Override
  public long getTimestampEpochNanos() {
    return audit.getTimestampEpochNanos();
  }

  @Override
  public long getObservedTimestampEpochNanos() {
    return audit.getObservedTimestampEpochNanos();
  }

  @Override
  public SpanContext getSpanContext() {
    return SpanContext.getInvalid();
  }

  /** Audit records do not use severity. */
  @Override
  public Severity getSeverity() {
    return Severity.UNDEFINED_SEVERITY_NUMBER;
  }

  @Override
  @Nullable
  public String getSeverityText() {
    return null;
  }

  @Override
  @Deprecated
  public Body getBody() {
    return audit.getBody() != null ? Body.string(audit.getBody().asString()) : Body.empty();
  }

  @Override
  @Nullable
  public Value<?> getBodyValue() {
    return audit.getBody();
  }

  @Override
  public Attributes getAttributes() {
    return mergedAttributes;
  }

  @Override
  public int getTotalAttributeCount() {
    return mergedAttributes.size();
  }

  @Override
  public String getEventName() {
    return audit.getEventName();
  }
}
