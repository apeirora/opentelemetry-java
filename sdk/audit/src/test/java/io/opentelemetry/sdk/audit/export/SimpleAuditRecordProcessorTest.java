/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditLogger;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.sdk.audit.AuditExportResult;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.AuditRecordExporter;
import io.opentelemetry.sdk.audit.SdkAuditProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SimpleAuditRecordProcessorTest {

  @Test
  void exportsRecordSynchronously() {
    InMemoryAuditRecordExporter exporter = InMemoryAuditRecordExporter.create();
    try (SdkAuditProvider provider =
        SdkAuditProvider.builder()
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exporter))
            .build()) {

      provider
          .get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("simple.test")
          .setActor("u1", ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();

      assertThat(exporter.getFinishedAuditRecords()).hasSize(1);
    }
  }

  @Test
  void throwsDeliveryException_onExporterFailure() {
    AuditRecordExporter failingExporter =
        new AuditRecordExporter() {
          @Override
          public AuditExportResult export(Collection<AuditRecordData> records) {
            return AuditExportResult.failure(new RuntimeException("sink unavailable"));
          }

          @Override
          public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
          }

          @Override
          public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
          }
        };

    try (SdkAuditProvider provider =
        SdkAuditProvider.builder()
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(failingExporter))
            .build()) {
      AuditLogger logger = provider.get("test");
      assertThatThrownBy(
              () ->
                  logger
                      .auditRecordBuilder()
                      .setTimestamp(Instant.now())
                      .setEventName("fail.test")
                      .setActor("u1", ActorType.USER)
                      .setAction("READ")
                      .setOutcome(Outcome.FAILURE)
                      .emit())
          .isInstanceOf(AuditDeliveryException.class);
    }
  }

  @Test
  void throwsDeliveryException_afterShutdown() {
    InMemoryAuditRecordExporter exporter = InMemoryAuditRecordExporter.create();
    SimpleAuditRecordProcessor processor = SimpleAuditRecordProcessor.create(exporter);
    try (SdkAuditProvider provider =
        SdkAuditProvider.builder().addAuditRecordProcessor(processor).build()) {
      provider.shutdown().join(5, TimeUnit.SECONDS);

      assertThatThrownBy(
              () ->
                  provider
                      .get("test")
                      .auditRecordBuilder()
                      .setTimestamp(Instant.now())
                      .setEventName("after.shutdown")
                      .setActor("u1", ActorType.USER)
                      .setAction("READ")
                      .setOutcome(Outcome.SUCCESS)
                      .emit())
          .isInstanceOf(AuditDeliveryException.class);
    }
  }

  @Test
  void exporterCalledOnce_perEmit() {
    AtomicInteger exportCallCount = new AtomicInteger(0);
    AuditRecordExporter countingExporter =
        new AuditRecordExporter() {
          @Override
          public AuditExportResult export(Collection<AuditRecordData> records) {
            exportCallCount.incrementAndGet();
            return AuditExportResult.success(
                Collections.singletonList(
                    AuditReceipt.create(records.iterator().next().getRecordId(), "", 0)));
          }

          @Override
          public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
          }

          @Override
          public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
          }
        };

    try (SdkAuditProvider provider =
        SdkAuditProvider.builder()
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(countingExporter))
            .build()) {
      for (int i = 0; i < 5; i++) {
        provider
            .get("test")
            .auditRecordBuilder()
            .setTimestamp(Instant.now())
            .setEventName("count.event")
            .setActor("u" + i, ActorType.USER)
            .setAction("READ")
            .setOutcome(Outcome.SUCCESS)
            .emit();
      }
      assertThat(exportCallCount.get()).isEqualTo(5);
    }
  }
}
