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

  /** Returns the identity of the actor that performed the action. */
  Value<?> getActor();

  /** Returns the type of the actor. */
  ActorType getActorType();

  /** Returns the action verb, e.g. {@code "LOGIN"}, {@code "DELETE"}. */
  String getAction();

  /** Returns the outcome of the action. */
  Outcome getOutcome();

  /** Returns the target resource of the action, or {@code null} if not set. */
  @Nullable
  Value<?> getTargetResource();

  /** Returns the source IP address, or {@code null} if not set. */
  @Nullable
  String getSourceIp();

  /** Returns the free-form body, or {@code null} if not set. */
  @Nullable
  Value<?> getBody();

  /** Returns the attributes attached to this record (never null; may be empty). */
  Attributes getAttributes();

  /** Returns the optional digital signature bytes, or {@code null} if not set. */
  @Nullable
  byte[] getSignature();

  /** Returns the signature algorithm, or {@code null} if {@link #getSignature()} is not set. */
  @Nullable
  String getAlgorithm();

  /** Returns the DER-encoded X.509 certificate, or {@code null} if not set. */
  @Nullable
  byte[] getCertificate();

  /** Returns the HMAC bytes, or {@code null} if not set. */
  @Nullable
  byte[] getHmac();

  /** Returns the HMAC algorithm, or {@code null} if {@link #getHmac()} is not set. */
  @Nullable
  String getHmacAlgorithm();

  /**
   * Returns the monotonic sequence number for hash-chain continuity, or {@code 0} if not set.
   */
  long getSequenceNo();

  /** Returns the {@code IntegrityHash} of the preceding record for hash-chain linking, or {@code
   * null} if not set. */
  @Nullable
  String getPrevHash();

  /** Returns the schema version of the audit payload, or {@code null} if not set. */
  @Nullable
  String getSchemaVersion();
}
