/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import javax.annotation.Nullable;

/**
 * Immutable representation of an audit record for use by processors and exporters.
 *
 * <p>Instances are created internally by the SDK when {@link
 * io.opentelemetry.api.audit.AuditRecordBuilder#emit()} is called.
 */
public interface AuditRecordData extends LogRecordData {

  // Audit records do not use LogRecordData's instrumentation scope, span context, severity, or body
  // fields. These defaults prevent AutoValue from generating constructor parameters for them.

  @Override
  default InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return InstrumentationScopeInfo.empty();
  }

  @Override
  default SpanContext getSpanContext() {
    return SpanContext.getInvalid();
  }

  @Override
  default Severity getSeverity() {
    return Severity.UNDEFINED_SEVERITY_NUMBER;
  }

  @Override
  @Nullable
  default String getSeverityText() {
    return null;
  }

  @Override
  @Deprecated
  default Body getBody() {
    return Body.empty();
  }

  @Override
  default int getTotalAttributeCount() {
    return getAttributes().size();
  }

  /** Returns the semantic event name for this audit record. Never null or empty. */
  @Override
  String getEventName();

  /**
   * Returns the diagnostic name of the {@link io.opentelemetry.api.audit.AuditLogger} that emitted
   * this record (for example {@code "com.example.auth"}).
   */
  String getLoggerName();

  /** Returns the optional version of the emitting component, or {@code null} if not set. */
  @Nullable
  String getLoggerVersion();

  /** Returns the optional schema URL, or {@code null} if not set. */
  @Nullable
  String getSchemaUrl();

  /** Returns the caller-generated unique identifier for this record. Never null or empty. */
  String getRecordId();

  /** Returns the identity of the actor ({@code audit.actor.id}). */
  String getActorId();

  /** Returns the type of the actor ({@code audit.actor.type}). */
  ActorType getActorType();

  /** Returns the action verb ({@code audit.action}), e.g. {@code "LOGIN"}, {@code "DELETE"}. */
  String getAction();

  /** Returns the outcome of the action ({@code audit.outcome}). */
  Outcome getOutcome();

  /** Returns the {@code audit.target.id}, or {@code null} if not set. */
  @Nullable
  String getTargetId();

  /** Returns the {@code audit.target.type}, or {@code null} if not set. */
  @Nullable
  String getTargetType();

  /** Returns the {@code audit.source.id}, or {@code null} if not set. */
  @Nullable
  String getSourceId();

  /** Returns the {@code audit.source.type}, or {@code null} if not set. */
  @Nullable
  String getSourceType();

  /**
   * Returns the raw bytes of the cryptographic integrity proof ({@code audit.integrity.value}), or
   * {@code null} if not set. The algorithm is carried as the {@code audit.integrity.algorithm}
   * Resource attribute.
   */
  @SuppressWarnings("mutable")
  @Nullable
  byte[] getIntegrityValue();

  /** Returns the monotonic sequence number for hash-chain continuity, or {@code 0} if not set. */
  long getSequenceNo();

  /**
   * Returns the {@code audit.prev.hash} of the preceding record for hash-chain linking, or {@code
   * null} if not set.
   */
  @Nullable
  String getPrevHash();

  /** Returns the {@code audit.schema.version}, or {@code null} if not set. */
  @Nullable
  String getSchemaVersion();
}
