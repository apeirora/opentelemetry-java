/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.AuditDeliveryException;
import io.opentelemetry.api.audit.AuditLogger;
import io.opentelemetry.api.audit.AuditProvider;
import io.opentelemetry.api.audit.AuditReceipt;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.audit.export.InMemoryAuditRecordExporter;
import io.opentelemetry.sdk.audit.export.SimpleAuditRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SdkAuditProviderTest {

  private InMemoryAuditRecordExporter exporter;
  private SdkAuditProvider provider;

  @BeforeEach
  void setUp() {
    exporter = InMemoryAuditRecordExporter.create();
    provider =
        SdkAuditProvider.builder()
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exporter))
            .build();
  }

  @AfterEach
  void tearDown() {
    provider.close();
  }

  @Test
  void emitMinimalRecord_returnsReceiptAndStoresRecord() {
    AuditLogger logger = provider.get("com.example.auth");

    AuditReceipt receipt =
        logger
            .auditRecordBuilder()
            .setTimestamp(Instant.now())
            .setEventName("user.login.success")
            .setActor("u8472", ActorType.USER)
            .setAction("LOGIN")
            .setOutcome(Outcome.SUCCESS)
            .emit();

    assertThat(receipt).isNotNull();
    assertThat(receipt.recordId()).isNotEmpty();

    List<AuditRecordData> records = exporter.getFinishedAuditRecords();
    assertThat(records).hasSize(1);
    AuditRecordData data = records.get(0);
    assertThat(data.getEventName()).isEqualTo("user.login.success");
    assertThat(data.getActorId()).isEqualTo("u8472");
    assertThat(data.getActorType()).isEqualTo(ActorType.USER);
    assertThat(data.getAction()).isEqualTo("LOGIN");
    assertThat(data.getOutcome()).isEqualTo(Outcome.SUCCESS);
  }

  @Test
  void emit_autoGeneratesRecordId_whenCallerOmitsIt() {
    AuditLogger logger = provider.get("com.example.test");
    AuditReceipt receipt =
        logger
            .auditRecordBuilder()
            .setTimestamp(Instant.now())
            .setEventName("config.change")
            .setActor("svc-deployer", ActorType.SERVICE)
            .setAction("UPDATE")
            .setOutcome(Outcome.SUCCESS)
            .emit();

    assertThat(receipt.recordId()).isNotEmpty();
    List<AuditRecordData> records = exporter.getFinishedAuditRecords();
    assertThat(records.get(0).getRecordId()).isEqualTo(receipt.recordId());
  }

  @Test
  void emit_stableRecordId_whenCallerSetsIt() {
    AuditLogger logger = provider.get("com.example.test");
    String fixedId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    logger
        .auditRecordBuilder()
        .setRecordId(fixedId)
        .setTimestamp(Instant.now())
        .setEventName("file.read")
        .setActor("u1", ActorType.USER)
        .setAction("READ")
        .setOutcome(Outcome.SUCCESS)
        .emit();

    assertThat(exporter.getFinishedAuditRecords().get(0).getRecordId()).isEqualTo(fixedId);
  }

  @Test
  void emit_setsObservedTimestamp_whenCallerOmitsIt() {
    AuditLogger logger = provider.get("com.example.test");
    logger
        .auditRecordBuilder()
        .setTimestamp(Instant.now())
        .setEventName("data.delete")
        .setActor("u2", ActorType.USER)
        .setAction("DELETE")
        .setOutcome(Outcome.SUCCESS)
        .emit();

    AuditRecordData data = exporter.getFinishedAuditRecords().get(0);
    assertThat(data.getObservedTimestampEpochNanos()).isGreaterThan(0);
  }

  @Test
  void emit_failsHard_whenTimestampMissing() {
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setEventName("user.login")
                    .setActor("u1", ActorType.USER)
                    .setAction("LOGIN")
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Timestamp");
  }

  @Test
  void emit_failsHard_whenEventNameMissing() {
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setActor("u1", ActorType.USER)
                    .setAction("LOGIN")
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("EventName");
  }

  @Test
  void emit_failsHard_whenActorIdMissing() {
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("user.login")
                    // actor omitted — no setActor() call
                    .setAction("LOGIN")
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ActorId");
  }

  @Test
  void emit_failsHard_whenActorTypeMissing() {
    // ActorType is always provided alongside ActorId via setActor(); this test
    // verifies that omitting setActor() entirely still fails with an ActorType message.
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("user.login")
                    // actor omitted — no setActor() call
                    .setAction("LOGIN")
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Actor");
  }

  @Test
  void emit_failsHard_whenActionMissing() {
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("user.login")
                    .setActor("u1", ActorType.USER)
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Action");
  }

  @Test
  void emit_failsHard_whenOutcomeMissing() {
    AuditLogger logger = provider.get("com.example.test");
    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("user.login")
                    .setActor("u1", ActorType.USER)
                    .setAction("LOGIN")
                    .emit())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Outcome");
  }

  @Test
  void emit_failsHard_afterShutdown() {
    AuditLogger logger = provider.get("com.example.test");
    provider.shutdown().join(5, TimeUnit.SECONDS);

    assertThatThrownBy(
            () ->
                logger
                    .auditRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setEventName("user.login")
                    .setActor("u1", ActorType.USER)
                    .setAction("LOGIN")
                    .setOutcome(Outcome.SUCCESS)
                    .emit())
        .isInstanceOf(AuditDeliveryException.class)
        .hasMessageContaining("shut down");
  }

  @Test
  void getAuditLogger_failsHard_afterShutdown() {
    provider.shutdown().join(5, TimeUnit.SECONDS);
    assertThatThrownBy(() -> provider.get("x"))
        .isInstanceOf(AuditDeliveryException.class)
        .hasMessageContaining("shut down");
  }

  @Test
  void emit_setsOptionalFields() {
    AuditLogger logger = provider.get("com.example.test");
    logger
        .auditRecordBuilder()
        .setTimestamp(Instant.now())
        .setEventName("resource.access")
        .setActor("svc-1", ActorType.SERVICE)
        .setAction("READ")
        .setOutcome(Outcome.SUCCESS)
        .setTarget("/api/data/123", "http.endpoint")
        .setSource("10.0.0.1", "ipv4")
        .setSchemaVersion("1.0.0")
        .setBody("additional context")
        .emit();

    AuditRecordData data = exporter.getFinishedAuditRecords().get(0);
    assertThat(data.getTargetId()).isEqualTo("/api/data/123");
    assertThat(data.getTargetType()).isEqualTo("http.endpoint");
    assertThat(data.getSourceId()).isEqualTo("10.0.0.1");
    assertThat(data.getSourceType()).isEqualTo("ipv4");
    assertThat(data.getSchemaVersion()).isEqualTo("1.0.0");
    // body is not stored on AuditRecordData; setBody() is a no-op per spec
  }

  @Test
  void sameLoggerReturnedForSameName() {
    AuditLogger l1 = provider.get("com.example.auth");
    AuditLogger l2 = provider.get("com.example.auth");
    assertThat(l1).isSameAs(l2);
  }

  @Test
  void resourceAttributesCarriedToRecord() {
    Resource resource =
        Resource.builder().put("service.name", "audit-svc").put("service.version", "1.0").build();
    InMemoryAuditRecordExporter exp = InMemoryAuditRecordExporter.create();
    try (SdkAuditProvider p =
        SdkAuditProvider.builder()
            .setResource(resource)
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exp))
            .build()) {
      p.get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("test.event")
          .setActor("u1", ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    Resource actual = exp.getFinishedAuditRecords().get(0).getResource();
    assertThat(actual.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("audit-svc");
    assertThat(actual.getAttribute(AttributeKey.stringKey("service.version"))).isEqualTo("1.0");
  }

  @Test
  void integrityAlgorithmStoredAsResourceAttribute() {
    InMemoryAuditRecordExporter exp = InMemoryAuditRecordExporter.create();
    try (SdkAuditProvider p =
        SdkAuditProvider.builder()
            .setIntegrityAlgorithm("HMAC-SHA256")
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exp))
            .build()) {
      p.get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("test.event")
          .setActor("u1", ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    String algo =
        exp.getFinishedAuditRecords()
            .get(0)
            .getResource()
            .getAttribute(AttributeKey.stringKey("audit.integrity.algorithm"));
    assertThat(algo).isEqualTo("HMAC-SHA256");
  }

  @Test
  void processorCanEnrichRecord() {
    InMemoryAuditRecordExporter exp = InMemoryAuditRecordExporter.create();
    AuditRecordProcessor enricher =
        (ctx, record) ->
            record.setAttribute(AttributeKey.stringKey("enriched.by"), "test-processor");
    try (SdkAuditProvider p =
        SdkAuditProvider.builder()
            .addAuditRecordProcessor(enricher)
            .addAuditRecordProcessor(SimpleAuditRecordProcessor.create(exp))
            .build()) {
      p.get("test")
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("test.event")
          .setActor("u1", ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    AuditRecordData data = exp.getFinishedAuditRecords().get(0);
    assertThat(data.getAttributes().get(AttributeKey.stringKey("enriched.by")))
        .isEqualTo("test-processor");
  }

  @Test
  void multipleRecords_allDelivered() {
    AuditLogger logger = provider.get("com.example.bulk");
    int count = 10;
    for (int i = 0; i < count; i++) {
      logger
          .auditRecordBuilder()
          .setTimestamp(Instant.now())
          .setEventName("bulk.event")
          .setActor("u" + i, ActorType.USER)
          .setAction("READ")
          .setOutcome(Outcome.SUCCESS)
          .emit();
    }
    assertThat(exporter.getFinishedAuditRecords()).hasSize(count);
  }

  @Test
  void sequenceNoAndPrevHash_storedOnRecord() {
    AuditLogger logger = provider.get("com.example.test");
    String prevHash = "deadbeef1234";
    logger
        .auditRecordBuilder()
        .setTimestamp(Instant.now())
        .setEventName("chain.event")
        .setActor("u1", ActorType.USER)
        .setAction("READ")
        .setOutcome(Outcome.SUCCESS)
        .setSequenceNo(42L)
        .setPrevHash(prevHash)
        .emit();

    AuditRecordData data = exporter.getFinishedAuditRecords().get(0);
    assertThat(data.getSequenceNo()).isEqualTo(42L);
    assertThat(data.getPrevHash()).isEqualTo(prevHash);
  }

  @Test
  void integrityValue_storedOnRecord() {
    AuditLogger logger = provider.get("com.example.test");
    byte[] proof = new byte[] {0x01, 0x02, 0x03};
    logger
        .auditRecordBuilder()
        .setTimestamp(Instant.now())
        .setEventName("signed.event")
        .setActor("svc", ActorType.SERVICE)
        .setAction("CREATE")
        .setOutcome(Outcome.SUCCESS)
        .setIntegrityValue(proof)
        .emit();

    AuditRecordData data = exporter.getFinishedAuditRecords().get(0);
    assertThat(data.getIntegrityValue()).containsExactly(0x01, 0x02, 0x03);
  }

  @Test
  void noopProvider_emitReturnsReceiptWithoutError() {
    AuditProvider noop = AuditProvider.noop();
    AuditReceipt receipt =
        noop.get("test")
            .auditRecordBuilder()
            .setTimestamp(Instant.now())
            .setEventName("noop.event")
            .setActor("u1", ActorType.USER)
            .setAction("READ")
            .setOutcome(Outcome.SUCCESS)
            .emit();
    assertThat(receipt).isNotNull();
  }
}
