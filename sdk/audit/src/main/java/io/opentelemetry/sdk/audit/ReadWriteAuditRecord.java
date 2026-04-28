/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import javax.annotation.Nullable;

/**
 * Mutable view of an {@link AuditRecordData} passed to {@link AuditRecordProcessor#onEmit}.
 *
 * <p>Processors MAY enrich the record by calling {@link #setAttribute}. They MUST NOT modify the
 * mandatory audit fields ({@code EventName}, {@code Actor}, {@code ActorType}, {@code Action},
 * {@code Outcome}): those are exposed as read-only accessors.
 *
 * <p>The {@link #setReceipt}/{@link #getReceipt} pair is used internally by the SDK to thread the
 * {@link AuditReceipt} returned by the exporter back to the calling {@code emit()} invocation.
 */
public interface ReadWriteAuditRecord {

  /**
   * Adds or replaces an attribute on this record. A {@code null} value is a no-op.
   *
   * <p>MUST NOT be called to modify mandatory fields; only additional enrichment attributes are
   * permitted.
   */
  <T> ReadWriteAuditRecord setAttribute(AttributeKey<T> key, @Nullable T value);

  /**
   * Stores the {@link AuditReceipt} returned by the exporter. Called by the SDK after a successful
   * export.
   */
  void setReceipt(AuditReceipt receipt);

  /** Returns the {@link AuditReceipt} set by the exporter, or {@code null} if not yet set. */
  @Nullable
  AuditReceipt getReceipt();

  /** Snapshots this record into an immutable {@link AuditRecordData} for export. */
  AuditRecordData toAuditRecordData();

  // ── Read-only accessors for mandatory fields ──────────────────────────────

  String getRecordId();

  long getTimestampEpochNanos();

  String getEventName();

  Value<?> getActor();

  ActorType getActorType();

  String getAction();

  Outcome getOutcome();
}
