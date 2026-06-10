/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.SdkAuditProvider;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchAuditRecordProcessorTest {

  private InMemoryAuditRecordExporter exporter;
  private BatchAuditRecordProcessor processor;
  private SdkAuditProvider provider;

  @BeforeEach
  void setUp() {
    exporter = InMemoryAuditRecordExporter.create();
    processor =
        BatchAuditRecordProcessor.builder(exporter)
            .setScheduledDelayMillis(100)
            .setMaxQueueSize(1000)
            .setMaxExportBatchSize(50)
            .build();
    provider = SdkAuditProvider.builder().addAuditRecordProcessor(processor).build();
  }

  @AfterEach
  void tearDown() {
    provider.close();
  }

  @Test
  void batchExports_allRecordsDelivered() throws InterruptedException {
    int count = 20;
    CountDownLatch latch = new CountDownLatch(count);

    for (int i = 0; i < count; i++) {
      int idx = i;
      Thread t =
          new Thread(
              () -> {
                provider
                    .get("test")
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("batch.event")
                    .setActor("u" + idx, ActorType.USER)
                    .setAction("READ")
                    .setOutcome(Outcome.SUCCESS)
                    .emit();
                latch.countDown();
              });
      t.start();
    }

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(exporter.getFinishedAuditRecords()).hasSize(count);
  }

  @Test
  void forceFlush_exportsAllPendingRecords() {
    int count = 5;
    for (int i = 0; i < count; i++) {
      provider
          .get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("flush.event")
          .setActor("u" + i, ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    provider.forceFlush().join(5, TimeUnit.SECONDS);
    assertThat(exporter.getFinishedAuditRecords()).hasSize(count);
  }

  @Test
  void receipts_returnedToCallers() {
    for (int i = 0; i < 3; i++) {
      AuditReceipt receipt =
          provider
              .get("test")
              .auditRecordBuilder()
              .setTimestamp(Instant.now())
              .setEventName("receipt.event")
              .setActor("u" + i, ActorType.USER)
              .setAction("READ")
              .setOutcome(Outcome.SUCCESS)
              .emit();
      assertThat(receipt).isNotNull();
      assertThat(receipt.recordId()).isNotEmpty();
    }
  }

  @Test
  void recordIds_areUnique_acrossBatch() {
    int count = 10;
    for (int i = 0; i < count; i++) {
      provider
          .get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("unique.event")
          .setActor("u" + i, ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    List<AuditRecordData> records = exporter.getFinishedAuditRecords();
    long distinctIds = records.stream().map(AuditRecordData::getRecordId).distinct().count();
    assertThat(distinctIds).isEqualTo(count);
  }

  @Test
  void shutdown_exportsRemainingRecords() {
    for (int i = 0; i < 3; i++) {
      provider
          .get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("shutdown.event")
          .setActor("u" + i, ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    provider.shutdown().join(10, TimeUnit.SECONDS);
    assertThat(exporter.getFinishedAuditRecords()).hasSize(3);
  }
}
