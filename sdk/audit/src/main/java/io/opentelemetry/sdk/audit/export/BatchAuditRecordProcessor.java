/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.audit.AuditExportResult;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.audit.AuditRecordProcessor;
import io.opentelemetry.sdk.audit.ReadWriteAuditRecord;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AuditRecordProcessor} that batches records for efficient export while still blocking
 * the calling thread until the batch containing the record is acknowledged by the audit sink.
 *
 * <p>Records are never dropped when the queue is full: the calling thread is blocked (back-pressure
 * is applied to the application) until there is space in the queue or the configured timeout
 * elapses.
 *
 * <p>Build via {@link BatchAuditRecordProcessorBuilder}:
 *
 * <pre>{@code
 * BatchAuditRecordProcessor processor = BatchAuditRecordProcessor.builder(exporter)
 *     .setMaxQueueSize(4096)
 *     .setScheduledDelayMillis(1000)
 *     .build();
 * }</pre>
 */
public final class BatchAuditRecordProcessor implements AuditRecordProcessor {

  private static final Logger logger = Logger.getLogger(BatchAuditRecordProcessor.class.getName());

  private final AuditRecordExporter exporter;
  private final int maxExportBatchSize;
  private final long exportTimeoutMillis;
  private final long scheduledDelayMillis;
  private final int maxRetryCount;
  private final long initialBackoffMillis;

  // Queue of pending records. BlockingQueue to apply back-pressure when full.
  private final BlockingQueue<PendingRecord> queue;

  private final AtomicBoolean isShutdown = new AtomicBoolean(false);
  private final AtomicReference<CompletableResultCode> flushRequested = new AtomicReference<>();
  private final BlockingQueue<Boolean> signal = new ArrayBlockingQueue<>(1);

  private final Thread worker;

  BatchAuditRecordProcessor(
      AuditRecordExporter exporter,
      int maxQueueSize,
      int maxExportBatchSize,
      long scheduledDelayMillis,
      long exportTimeoutMillis,
      int maxRetryCount,
      long initialBackoffMillis) {
    this.exporter = exporter;
    this.maxExportBatchSize = maxExportBatchSize;
    this.scheduledDelayMillis = scheduledDelayMillis;
    this.exportTimeoutMillis = exportTimeoutMillis;
    this.maxRetryCount = maxRetryCount;
    this.initialBackoffMillis = initialBackoffMillis;
    this.queue = new ArrayBlockingQueue<>(maxQueueSize);
    this.worker = new Thread(new Worker(), "otel-audit-batch-worker");
    this.worker.setDaemon(true);
    this.worker.start();
  }

  /** Returns a new {@link BatchAuditRecordProcessorBuilder} for the given exporter. */
  public static BatchAuditRecordProcessorBuilder builder(AuditRecordExporter exporter) {
    return new BatchAuditRecordProcessorBuilder(exporter);
  }

  @Override
  public void onEmit(Context context, ReadWriteAuditRecord record) {
    if (isShutdown.get()) {
      throw new AuditDeliveryException(
          "BatchAuditRecordProcessor has been shut down; refusing to emit record");
    }
    CompletableFuture<AuditReceipt> future = new CompletableFuture<>();
    PendingRecord pending = new PendingRecord(record.toAuditRecordData(), future);
    // Block until there is space in the queue (back-pressure as per spec)
    boolean offered = false;
    try {
      offered = queue.offer(pending, exportTimeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AuditDeliveryException("Interrupted while enqueuing audit record", e);
    }
    if (!offered) {
      throw new AuditDeliveryException(
          "Audit record queue is full and back-pressure timeout elapsed; record rejected");
    }
    // Signal the worker in case it is waiting for work
    signal.offer(Boolean.TRUE);
    // Block until the worker exports the batch and completes the future
    try {
      AuditReceipt receipt = future.get(exportTimeoutMillis, TimeUnit.MILLISECONDS);
      record.setReceipt(receipt);
    } catch (TimeoutException e) {
      future.cancel(false);
      throw new AuditDeliveryException(
          "Timed out waiting for audit export acknowledgement after " + exportTimeoutMillis + "ms",
          e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof AuditDeliveryException) {
        throw (AuditDeliveryException) cause;
      }
      throw new AuditDeliveryException("Audit export failed", cause != null ? cause : e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AuditDeliveryException("Interrupted while waiting for audit export", e);
    }
  }

  @Override
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      return CompletableResultCode.ofSuccess();
    }
    CompletableResultCode result = forceFlush();
    result.whenComplete(exporter::shutdown);
    return result;
  }

  @Override
  public CompletableResultCode forceFlush() {
    CompletableResultCode flushResult = new CompletableResultCode();
    if (flushRequested.compareAndSet(null, flushResult)) {
      signal.offer(Boolean.TRUE);
    }
    CompletableResultCode existing = flushRequested.get();
    return existing != null ? existing : flushResult;
  }

  // ── Worker ────────────────────────────────────────────────────────────────

  private final class Worker implements Runnable {

    @Override
    public void run() {
      List<PendingRecord> batch = new ArrayList<>(maxExportBatchSize);
      while (!isShutdown.get()) {
        try {
          // Wait for work or the scheduled delay
          signal.poll(scheduledDelayMillis, TimeUnit.MILLISECONDS);
          exportBatch(batch);
          CompletableResultCode flush = flushRequested.getAndSet(null);
          if (flush != null) {
            flush.succeed();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      // Drain remaining records on shutdown
      exportBatch(batch);
    }

    private void exportBatch(List<PendingRecord> batch) {
      batch.clear();
      queue.drainTo(batch, maxExportBatchSize);
      if (batch.isEmpty()) {
        return;
      }
      List<AuditRecordData> records = new ArrayList<>(batch.size());
      for (PendingRecord p : batch) {
        records.add(p.data);
      }
      AuditExportResult result = exportWithRetry(records);
      if (result.isSuccess()) {
        List<AuditReceipt> receipts = result.getReceipts();
        for (int i = 0; i < batch.size(); i++) {
          AuditReceipt receipt = i < receipts.size() ? receipts.get(i) : null;
          if (receipt != null) {
            batch.get(i).future.complete(receipt);
          } else {
            batch
                .get(i)
                .future
                .completeExceptionally(
                    new AuditDeliveryException("Exporter returned no receipt for record " + i));
          }
        }
      } else {
        AuditDeliveryException ex =
            new AuditDeliveryException(
                "Audit export failed after " + maxRetryCount + " retries",
                result.getFailure() != null
                    ? result.getFailure()
                    : new IllegalStateException("Audit export failed without cause"));
        for (PendingRecord p : batch) {
          p.future.completeExceptionally(ex);
        }
      }
    }

    private AuditExportResult exportWithRetry(List<AuditRecordData> records) {
      long backoff = initialBackoffMillis;
      for (int attempt = 0; attempt <= maxRetryCount; attempt++) {
        AuditExportResult result = exporter.export(records);
        if (result.isSuccess()) {
          return result;
        }
        if (attempt < maxRetryCount) {
          logger.log(
              Level.WARNING,
              "Audit export attempt {0} failed; retrying in {1}ms",
              new Object[] {attempt + 1, backoff});
          try {
            Thread.sleep(backoff);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AuditExportResult.failure(e);
          }
          backoff = Math.min(backoff * 2, exportTimeoutMillis);
        }
      }
      return AuditExportResult.failure(
          new AuditDeliveryException("Max retries exhausted: " + maxRetryCount));
    }
  }

  // ── Holder ────────────────────────────────────────────────────────────────

  private static final class PendingRecord {
    final AuditRecordData data;
    final CompletableFuture<AuditReceipt> future;

    PendingRecord(AuditRecordData data, CompletableFuture<AuditReceipt> future) {
      this.data = data;
      this.future = future;
    }
  }
}
