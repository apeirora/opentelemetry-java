/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import io.opentelemetry.api.audit.AuditReceipt;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The synchronous result of an {@link AuditRecordExporter#export} call.
 *
 * <p>On success, {@link #getReceipts()} contains one {@link AuditReceipt} per exported record in
 * the same order as the input collection. On failure, the list is empty and {@link #isSuccess()}
 * returns {@code false}.
 */
public final class AuditExportResult {

  private final boolean success;
  private final List<AuditReceipt> receipts;
  @Nullable private final Throwable failure;

  private AuditExportResult(
      boolean success, List<AuditReceipt> receipts, @Nullable Throwable failure) {
    this.success = success;
    this.receipts = receipts;
    this.failure = failure;
  }

  /** Creates a successful result with the given receipts. */
  public static AuditExportResult success(List<AuditReceipt> receipts) {
    return new AuditExportResult(/* success= */ true, Collections.unmodifiableList(receipts), null);
  }

  /** Creates a failure result with the given cause. */
  public static AuditExportResult failure(Throwable cause) {
    return new AuditExportResult(/* success= */ false, Collections.emptyList(), cause);
  }

  /** Creates a failure result without a specific cause. */
  public static AuditExportResult failure() {
    return new AuditExportResult(/* success= */ false, Collections.emptyList(), null);
  }

  /** Returns {@code true} if all records were successfully acknowledged by the audit sink. */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Returns the {@link AuditReceipt}s returned by the audit sink, one per exported record. Empty if
   * {@link #isSuccess()} is {@code false}.
   */
  public List<AuditReceipt> getReceipts() {
    return receipts;
  }

  /**
   * Returns the cause of the failure, or {@code null} if the failure has no associated throwable or
   * if the export succeeded.
   */
  @Nullable
  public Throwable getFailure() {
    return failure;
  }
}
