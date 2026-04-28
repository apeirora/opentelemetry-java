/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.sdk.audit.AuditRecordExporter;

/** Builder for {@link BatchAuditRecordProcessor}. */
public final class BatchAuditRecordProcessorBuilder {

  static final int DEFAULT_MAX_QUEUE_SIZE = 2048;
  static final int DEFAULT_MAX_EXPORT_BATCH_SIZE = 512;
  static final long DEFAULT_SCHEDULED_DELAY_MILLIS = 5_000;
  static final long DEFAULT_EXPORT_TIMEOUT_MILLIS = 30_000;
  static final int DEFAULT_MAX_RETRY_COUNT = 5;
  static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 1_000;

  private final AuditRecordExporter exporter;
  private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
  private int maxExportBatchSize = DEFAULT_MAX_EXPORT_BATCH_SIZE;
  private long scheduledDelayMillis = DEFAULT_SCHEDULED_DELAY_MILLIS;
  private long exportTimeoutMillis = DEFAULT_EXPORT_TIMEOUT_MILLIS;
  private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
  private long initialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;

  BatchAuditRecordProcessorBuilder(AuditRecordExporter exporter) {
    this.exporter = requireNonNull(exporter, "exporter");
  }

  /**
   * Sets the maximum number of records held in the queue before back-pressure is applied to the
   * calling thread. Default: {@value #DEFAULT_MAX_QUEUE_SIZE}.
   */
  public BatchAuditRecordProcessorBuilder setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
    return this;
  }

  /**
   * Sets the maximum number of records per exported batch. Default: {@value
   * #DEFAULT_MAX_EXPORT_BATCH_SIZE}.
   */
  public BatchAuditRecordProcessorBuilder setMaxExportBatchSize(int maxExportBatchSize) {
    this.maxExportBatchSize = maxExportBatchSize;
    return this;
  }

  /**
   * Sets the delay in milliseconds between two consecutive exports when the batch is not full.
   * Default: {@value #DEFAULT_SCHEDULED_DELAY_MILLIS} ms.
   */
  public BatchAuditRecordProcessorBuilder setScheduledDelayMillis(long scheduledDelayMillis) {
    this.scheduledDelayMillis = scheduledDelayMillis;
    return this;
  }

  /**
   * Sets the maximum time in milliseconds allowed for a single export call before it is considered
   * a failure. Default: {@value #DEFAULT_EXPORT_TIMEOUT_MILLIS} ms.
   */
  public BatchAuditRecordProcessorBuilder setExportTimeoutMillis(long exportTimeoutMillis) {
    this.exportTimeoutMillis = exportTimeoutMillis;
    return this;
  }

  /**
   * Sets the maximum number of export retry attempts before surfacing a hard error. Default:
   * {@value #DEFAULT_MAX_RETRY_COUNT}.
   */
  public BatchAuditRecordProcessorBuilder setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
    return this;
  }

  /**
   * Sets the initial back-off delay in milliseconds between retries; doubled on each attempt.
   * Default: {@value #DEFAULT_INITIAL_BACKOFF_MILLIS} ms.
   */
  public BatchAuditRecordProcessorBuilder setInitialBackoffMillis(long initialBackoffMillis) {
    this.initialBackoffMillis = initialBackoffMillis;
    return this;
  }

  /** Builds and returns the configured {@link BatchAuditRecordProcessor}. */
  public BatchAuditRecordProcessor build() {
    return new BatchAuditRecordProcessor(
        exporter,
        maxQueueSize,
        maxExportBatchSize,
        scheduledDelayMillis,
        exportTimeoutMillis,
        maxRetryCount,
        initialBackoffMillis);
  }
}
