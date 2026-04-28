/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.sdk.audit.AuditExportResult;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An in-memory {@link AuditRecordExporter} for use in tests.
 *
 * <p>Stores all exported {@link AuditRecordData}s in a list and returns synthetic {@link
 * AuditReceipt}s with empty integrity hashes (since there is no real audit sink).
 *
 * <pre>{@code
 * InMemoryAuditRecordExporter exporter = InMemoryAuditRecordExporter.create();
 * SdkAuditProvider provider = SdkAuditProvider.builder()
 *     .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exporter))
 *     .build();
 * // ...
 * List<AuditRecordData> records = exporter.getFinishedAuditRecords();
 * }</pre>
 */
public final class InMemoryAuditRecordExporter implements AuditRecordExporter {

  private final List<AuditRecordData> finishedRecords = new ArrayList<>();
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);
  private final Object lock = new Object();

  private InMemoryAuditRecordExporter() {}

  /** Creates a new {@link InMemoryAuditRecordExporter}. */
  public static InMemoryAuditRecordExporter create() {
    return new InMemoryAuditRecordExporter();
  }

  @Override
  public AuditExportResult export(Collection<AuditRecordData> records) {
    if (isShutdown.get()) {
      return AuditExportResult.failure(new IllegalStateException("Exporter has been shut down"));
    }
    List<AuditReceipt> receipts = new ArrayList<>(records.size());
    synchronized (lock) {
      for (AuditRecordData record : records) {
        finishedRecords.add(record);
        // Synthetic receipt: echo the recordId; integrity hash is empty (no real sink)
        receipts.add(AuditReceipt.create(record.getRecordId(), "", 0));
      }
    }
    return AuditExportResult.success(receipts);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    isShutdown.set(true);
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Returns an unmodifiable snapshot of all exported {@link AuditRecordData}s in the order they
   * were exported.
   */
  public List<AuditRecordData> getFinishedAuditRecords() {
    synchronized (lock) {
      return Collections.unmodifiableList(new ArrayList<>(finishedRecords));
    }
  }

  /** Clears the list of finished records. */
  public void reset() {
    synchronized (lock) {
      finishedRecords.clear();
    }
  }
}
