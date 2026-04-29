/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

/**
 * Immutable representation of an audit record for use by processors and exporters.
 *
 * <p>Instances are created internally by the SDK when {@link
 * io.opentelemetry.api.audit.AuditRecordBuilder#emit()} is called.
 */
public interface AuditRecordData {

  /** Returns the {@link Resource} of the emitting service. */
  Resource getResource();

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

  /** Returns the event time as nanoseconds since the UNIX epoch (UTC). */
  long getTimestampEpochNanos();

  /** Returns the SDK observation time as nanoseconds since the UNIX epoch (UTC). */
  long getObservedTimestampEpochNanos();

  /** Returns the semantic name of the audit event, e.g. {@code "user.login.success"}. */
  String getEventName();

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

  /** Returns the free-form body, or {@code null} if not set. */
  @Nullable
  Value<?> getBody();

  /** Returns the attributes attached to this record (never null; may be empty). */
  Attributes getAttributes();

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
