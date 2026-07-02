/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

/**
 * Semantic convention attribute names for OpenTelemetry audit records.
 *
 * <p>These constants correspond to the {@code audit.*} attributes defined by the audit record data
 * model.
 */
public final class Audit {

  /** Unique stable identifier for an audit record. */
  public static final String RECORD_ID = "audit.record.id";

  /** Identity of the actor that performed the action. */
  public static final String ACTOR_ID = "audit.actor.id";

  /** Type of the actor that performed the action. */
  public static final String ACTOR_TYPE = "audit.actor.type";

  /** Verb describing what the actor did. */
  public static final String ACTION = "audit.action";

  /** Result of the auditable action. */
  public static final String OUTCOME = "audit.outcome";

  /** Identifier of the target resource acted upon. */
  public static final String TARGET_ID = "audit.target.id";

  /** Type of the target resource acted upon. */
  public static final String TARGET_TYPE = "audit.target.type";

  /** Identifier of the source of the auditable action. */
  public static final String SOURCE_ID = "audit.source.id";

  /** Type of the source of the auditable action. */
  public static final String SOURCE_TYPE = "audit.source.type";

  /** Base64-encoded cryptographic integrity proof for the record. */
  public static final String INTEGRITY_VALUE = "audit.integrity.value";

  /** Monotonic sequence number used for hash-chain continuity. */
  public static final String SEQUENCE_NUMBER = "audit.sequence.number";

  /** Hash of the previous record in the same audit stream. */
  public static final String PREV_HASH = "audit.prev.hash";

  /** Schema version of the audit payload. */
  public static final String SCHEMA_VERSION = "audit.schema.version";

  /** Resource attribute naming the integrity algorithm used for emitted audit records. */
  public static final String INTEGRITY_ALGORITHM = "audit.integrity.algorithm";

  /** Resource attribute referencing the certificate or key used for integrity proofs. */
  public static final String INTEGRITY_CERTIFICATE = "audit.integrity.certificate";

  private Audit() {}
}
