/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.http.audit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
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
 *   <li>Mandatory audit fields are stored as {@code Attributes} with well-known keys.
 * </ul>
 */
final class AuditLogRecordDataAdapter implements LogRecordData {

  private static final String ATTR_RECORD_ID = "audit.record_id";
  private static final String ATTR_ACTOR = "audit.actor";
  private static final String ATTR_ACTOR_TYPE = "audit.actor_type";
  private static final String ATTR_ACTION = "audit.action";
  private static final String ATTR_OUTCOME = "audit.outcome";
  private static final String ATTR_TARGET_RESOURCE = "audit.target_resource";
  private static final String ATTR_SOURCE_IP = "audit.source_ip";
  private static final String ATTR_SCHEMA_VERSION = "audit.schema_version";
  private static final String ATTR_SEQUENCE_NO = "audit.sequence_no";
  private static final String ATTR_PREV_HASH = "audit.prev_hash";

  private final AuditRecordData audit;
  private final Attributes mergedAttributes;

  AuditLogRecordDataAdapter(AuditRecordData audit) {
    this.audit = audit;
    this.mergedAttributes = buildAttributes(audit);
  }

  private static Attributes buildAttributes(AuditRecordData a) {
    AttributesBuilder b = Attributes.builder();
    // Mandatory audit fields as attributes
    b.put(AttributeKey.stringKey(ATTR_RECORD_ID), a.getRecordId());
    b.put(AttributeKey.stringKey(ATTR_ACTOR), a.getActor().asString());
    b.put(AttributeKey.stringKey(ATTR_ACTOR_TYPE), a.getActorType().name());
    b.put(AttributeKey.stringKey(ATTR_ACTION), a.getAction());
    b.put(AttributeKey.stringKey(ATTR_OUTCOME), a.getOutcome().name());
    // Optional audit fields
    if (a.getTargetResource() != null) {
      b.put(AttributeKey.stringKey(ATTR_TARGET_RESOURCE), a.getTargetResource().asString());
    }
    if (a.getSourceIp() != null) {
      b.put(AttributeKey.stringKey(ATTR_SOURCE_IP), a.getSourceIp());
    }
    if (a.getSchemaVersion() != null) {
      b.put(AttributeKey.stringKey(ATTR_SCHEMA_VERSION), a.getSchemaVersion());
    }
    if (a.getSequenceNo() != 0) {
      b.put(AttributeKey.longKey(ATTR_SEQUENCE_NO), a.getSequenceNo());
    }
    if (a.getPrevHash() != null) {
      b.put(AttributeKey.stringKey(ATTR_PREV_HASH), a.getPrevHash());
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

  /** Audit records do not use severity; always returns {@code null}. */
  @Override
  @Nullable
  public Severity getSeverity() {
    return null;
  }

  @Override
  @Nullable
  public String getSeverityText() {
    return null;
  }

  @Override
  @Nullable
  public io.opentelemetry.api.common.Value<?> getBodyValue() {
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
