/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.audit.AuditExportResult;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.audit.AuditRecordProcessor;
import io.opentelemetry.sdk.audit.ReadWriteAuditRecord;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AuditRecordProcessor} that passes each {@link AuditRecordData} directly to the
 * configured {@link AuditRecordExporter} synchronously on the calling thread.
 *
 * <p>This is the default processor for synchronous {@code emit()} calls. It guarantees that {@code
 * emit()} blocks until the exporter has acknowledged the record and returns the {@link
 * AuditReceipt} from the sink.
 *
 * <p>For high-volume scenarios, consider {@link BatchAuditRecordProcessor}.
 */
public final class SimpleAuditRecordProcessor implements AuditRecordProcessor {

  private static final Logger logger = Logger.getLogger(SimpleAuditRecordProcessor.class.getName());

  private final AuditRecordExporter exporter;
  private final Object exporterLock = new Object();
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  private SimpleAuditRecordProcessor(AuditRecordExporter exporter) {
    this.exporter = exporter;
  }

  /**
   * Creates a new {@link SimpleAuditRecordProcessor} that synchronously exports to the given {@link
   * AuditRecordExporter}.
   */
  public static SimpleAuditRecordProcessor create(AuditRecordExporter exporter) {
    requireNonNull(exporter, "exporter");
    return new SimpleAuditRecordProcessor(exporter);
  }

  @Override
  public void onEmit(Context context, ReadWriteAuditRecord record) {
    if (isShutdown.get()) {
      throw new AuditDeliveryException(
          "SimpleAuditRecordProcessor has been shut down; refusing to emit record");
    }
    List<AuditRecordData> batch = Collections.singletonList(record.toAuditRecordData());
    AuditExportResult result;
    synchronized (exporterLock) {
      result = exporter.export(batch);
    }
    if (!result.isSuccess()) {
      Throwable cause = result.getFailure();
      String msg = "Audit record export failed";
      if (cause != null) {
        throw new AuditDeliveryException(msg, cause);
      }
      throw new AuditDeliveryException(msg);
    }
    List<AuditReceipt> receipts = result.getReceipts();
    if (receipts.isEmpty()) {
      throw new AuditDeliveryException(
          "Exporter returned success but no AuditReceipt; check exporter implementation");
    }
    record.setReceipt(receipts.get(0));
  }

  @Override
  public CompletableResultCode shutdown() {
    if (isShutdown.getAndSet(true)) {
      return CompletableResultCode.ofSuccess();
    }
    CompletableResultCode result = new CompletableResultCode();
    CompletableResultCode shutdownResult = exporter.shutdown();
    shutdownResult.whenComplete(
        () -> {
          if (shutdownResult.isSuccess()) {
            result.succeed();
          } else {
            logger.log(Level.WARNING, "Exporter failed to shut down cleanly");
            result.fail();
          }
        });
    return result;
  }

  @Override
  public CompletableResultCode forceFlush() {
    return exporter.flush();
  }

  /** Returns the configured {@link AuditRecordExporter}. */
  public AuditRecordExporter getExporter() {
    return exporter;
  }

  @Override
  public String toString() {
    return "SimpleAuditRecordProcessor{exporter=" + exporter + '}';
  }
}
