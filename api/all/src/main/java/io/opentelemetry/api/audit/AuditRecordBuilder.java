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

/**
 * Used to construct and emit {@link AuditReceipt}-returning audit records from an {@link
 * AuditLogger}.
 *
 * <p>Obtain an {@link AuditLogger#auditRecordBuilder()}, set all required and desired optional
 * fields, then call {@link #emit()} which blocks until the audit sink acknowledges the record and
 * returns an {@link AuditReceipt} as proof-of-delivery.
 *
 * <p>Unlike {@link io.opentelemetry.api.logs.LogRecordBuilder}, {@code emit()} returns a non-void
 * {@link AuditReceipt} and MUST NOT silently drop the record. An exception is raised if the sink
 * cannot be reached within the configured timeout.
 */
public interface AuditRecordBuilder {

  // ── Required fields ──────────────────────────────────────────────────────

  /**
   * Sets the caller-generated unique identifier for this record. If not set, the SDK MUST generate
   * a UUID v4. The value MUST remain stable across retries of the same event.
   */
  AuditRecordBuilder setRecordId(String recordId);

  /**
   * Sets the epoch timestamp (event time) using the given value and unit.
   *
   * <p>This field is required. It represents the time at which the auditable action occurred.
   */
  AuditRecordBuilder setTimestamp(long timestamp, TimeUnit unit);

  /** Sets the epoch timestamp (event time) using the given {@link Instant}. */
  AuditRecordBuilder setTimestamp(Instant instant);

  /**
   * Sets the semantic name that uniquely identifies the type of audit event, e.g. {@code
   * "user.login.success"}. MUST be non-empty and stable across releases.
   */
  AuditRecordBuilder setEventName(String eventName);

  /**
   * Sets the identity of the entity that performed the auditable action.
   *
   * <p>MAY be a structured value. If the actor cannot be determined, set to a sentinel such as
   * {@code "anonymous"}.
   */
  AuditRecordBuilder setActor(Value<?> actor);

  /** Convenience overload of {@link #setActor(Value)} accepting a plain string. */
  default AuditRecordBuilder setActor(String actor) {
    return setActor(Value.of(actor));
  }

  /** Sets the type of the actor. */
  AuditRecordBuilder setActorType(ActorType actorType);

  /**
   * Sets the verb that describes what the actor did, e.g. {@code "LOGIN"}, {@code "READ"}, {@code
   * "DELETE"}. MUST be non-empty and stable across releases.
   */
  AuditRecordBuilder setAction(String action);

  /** Sets the result of the auditable action. */
  AuditRecordBuilder setOutcome(Outcome outcome);

  // ── Optional fields ───────────────────────────────────────────────────────

  /**
   * Sets the epoch observed-timestamp using the given value and unit. If not set, the SDK MUST set
   * this to the wall-clock time at the moment {@link #emit()} is called.
   */
  AuditRecordBuilder setObservedTimestamp(long timestamp, TimeUnit unit);

  /** Sets the epoch observed-timestamp using the given {@link Instant}. */
  AuditRecordBuilder setObservedTimestamp(Instant instant);

  /** Sets the schema version of the audit payload, e.g. {@code "1.0.0"}. */
  AuditRecordBuilder setSchemaVersion(String schemaVersion);

  /**
   * Sets the object upon which the action was performed, e.g. a file path, database row, or
   * structured resource descriptor.
   */
  AuditRecordBuilder setTargetResource(Value<?> targetResource);

  /** Sets the source network address of the auditable action, e.g. {@code "203.0.113.42"}. */
  AuditRecordBuilder setSourceIp(String sourceIp);

  /** Sets free-form additional information about the audit event. */
  AuditRecordBuilder setBody(Value<?> body);

  /** Convenience overload of {@link #setBody(Value)} accepting a plain string. */
  default AuditRecordBuilder setBody(String body) {
    return setBody(Value.of(body));
  }

  /**
   * Sets an attribute on this record. If the record already contains a mapping for the key, the
   * old value is replaced.
   *
   * <p>Providing a {@code null} value is a no-op and does not remove previously set values.
   */
  <T> AuditRecordBuilder setAttribute(AttributeKey<T> key, @Nullable T value);

  /**
   * Sets an asymmetric digital signature over the canonical serialization of this record and the
   * algorithm used (e.g. {@code "ES256"}). MUST NOT be set together with {@link
   * #setHmac(byte[], String)}.
   */
  AuditRecordBuilder setSignature(byte[] signature, String algorithm);

  /**
   * Sets the DER-encoded X.509 public-key certificate corresponding to the signing key. Only
   * meaningful when {@link #setSignature(byte[], String)} is also set.
   */
  AuditRecordBuilder setCertificate(byte[] certificate);

  /**
   * Sets a symmetric HMAC over the canonical serialization of this record and the algorithm used
   * (e.g. {@code "HMAC-SHA256"}). MUST NOT be set together with {@link
   * #setSignature(byte[], String)}.
   */
  AuditRecordBuilder setHmac(byte[] hmac, String algorithm);

  /**
   * Sets the monotonically increasing sequence number for hash-chain continuity. When set,
   * receivers can detect gaps that indicate lost or deleted records.
   */
  AuditRecordBuilder setSequenceNo(long sequenceNo);

  /**
   * Sets the {@code IntegrityHash} of the immediately preceding record in the same audit stream,
   * enabling hash-chain validation.
   */
  AuditRecordBuilder setPrevHash(String prevHash);

  // ── Terminal ──────────────────────────────────────────────────────────────

  /**
   * Emits the audit record and blocks until the audit sink acknowledges receipt.
   *
   * <p>Returns an {@link AuditReceipt} containing the sink-assigned {@code RecordId}, {@code
   * IntegrityHash}, and {@code SinkTimestamp}.
   *
   * <p>If the sink cannot be reached within the configured timeout and the retry budget is
   * exhausted, this method MUST throw a runtime exception and MUST NOT return silently.
   *
   * @throws AuditDeliveryException if the audit sink cannot be reached and all retries are
   *     exhausted
   */
  AuditReceipt emit();
}
