/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import javax.annotation.concurrent.Immutable;

/**
 * Proof-of-delivery returned by {@link AuditLogger} once the audit sink has persisted the record.
 *
 * <p>The {@code recordId} echoes the caller's {@link AuditRecordBuilder#setRecordId(String)}.
 * {@code integrityHash} is the SHA-256 of the record as written by the sink. {@code
 * sinkTimestampEpochNanos} is the nanosecond UNIX epoch at which the sink persisted the record.
 */
@Immutable
public final class AuditReceipt {

  private final String recordId;
  private final String integrityHash;
  private final long sinkTimestampEpochNanos;

  private AuditReceipt(String recordId, String integrityHash, long sinkTimestampEpochNanos) {
    this.recordId = recordId;
    this.integrityHash = integrityHash;
    this.sinkTimestampEpochNanos = sinkTimestampEpochNanos;
  }

  /** Creates an {@link AuditReceipt} with the given fields. */
  public static AuditReceipt create(
      String recordId, String integrityHash, long sinkTimestampEpochNanos) {
    return new AuditReceipt(recordId, integrityHash, sinkTimestampEpochNanos);
  }

  /** Returns the {@code RecordId} echoed from the corresponding {@link AuditRecordBuilder}. */
  public String recordId() {
    return recordId;
  }

  /**
   * Returns the SHA-256 hex digest of the canonical serialization of the {@code AuditRecord} as
   * persisted by the audit sink.
   */
  public String integrityHash() {
    return integrityHash;
  }

  /** Returns the nanosecond UNIX epoch at which the audit sink persisted the record. */
  public long sinkTimestampEpochNanos() {
    return sinkTimestampEpochNanos;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AuditReceipt)) {
      return false;
    }
    AuditReceipt other = (AuditReceipt) obj;
    return recordId.equals(other.recordId)
        && integrityHash.equals(other.integrityHash)
        && sinkTimestampEpochNanos == other.sinkTimestampEpochNanos;
  }

  @Override
  public int hashCode() {
    int result = recordId.hashCode();
    result = 31 * result + integrityHash.hashCode();
    result = 31 * result + Long.hashCode(sinkTimestampEpochNanos);
    return result;
  }

  @Override
  public String toString() {
    return "AuditReceipt{"
        + "recordId="
        + recordId
        + ", integrityHash="
        + integrityHash
        + ", sinkTimestampEpochNanos="
        + sinkTimestampEpochNanos
        + "}";
  }
}
