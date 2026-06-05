/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.otlp.http.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.audit.AuditRecordData;
import io.opentelemetry.sdk.audit.SdkAuditRecordData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuditLogRecordDataAdapterTest {

  private static final long TIMESTAMP_NANOS = 1_714_041_600_000_000_000L;
  private static final long OBSERVED_NANOS = 1_714_041_600_001_000_000L;

  @Test
  void mandatoryAttributesMapped() {
    AuditRecordData data = buildMinimal();
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    Attributes attrs = adapted.getAttributes();

    assertThat(attrs.get(AttributeKey.stringKey("audit.record.id"))).isEqualTo("test-record-id");
    assertThat(attrs.get(AttributeKey.stringKey("audit.actor.id"))).isEqualTo("u8472");
    assertThat(attrs.get(AttributeKey.stringKey("audit.actor.type"))).isEqualTo("user");
    assertThat(attrs.get(AttributeKey.stringKey("audit.action"))).isEqualTo("LOGIN");
    assertThat(attrs.get(AttributeKey.stringKey("audit.outcome"))).isEqualTo("success");
  }

  @Test
  void actorType_isLowercase() {
    AuditRecordData data =
        SdkAuditRecordData.create(
            Resource.getDefault(), "test", null, null,
            "id1", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "test.event", "svc-1", ActorType.SERVICE,
            "CREATE", Outcome.SUCCESS,
            null, null, null, null, null, Attributes.empty(), null, 0, null, null);
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    assertThat(adapted.getAttributes().get(AttributeKey.stringKey("audit.actor.type")))
        .isEqualTo("service");
  }

  @Test
  void outcome_isLowercase() {
    AuditRecordData data =
        SdkAuditRecordData.create(
            Resource.getDefault(), "test", null, null,
            "id2", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "test.event", "sys", ActorType.SYSTEM,
            "REBOOT", Outcome.FAILURE,
            null, null, null, null, null, Attributes.empty(), null, 0, null, null);
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    assertThat(adapted.getAttributes().get(AttributeKey.stringKey("audit.outcome")))
        .isEqualTo("failure");
  }

  @Test
  void optionalAttributes_mappedWhenPresent() {
    AuditRecordData data =
        SdkAuditRecordData.create(
            Resource.getDefault(), "test", null, null,
            "id3", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "resource.access", "u1", ActorType.USER,
            "READ", Outcome.SUCCESS,
            "/api/data/123", "http.endpoint",
            "10.0.0.1", "ipv4",
            Value.of("body text"),
            Attributes.empty(), null, 42L, "prevhash123", "1.0.0");
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    Attributes attrs = adapted.getAttributes();

    assertThat(attrs.get(AttributeKey.stringKey("audit.target.id"))).isEqualTo("/api/data/123");
    assertThat(attrs.get(AttributeKey.stringKey("audit.target.type"))).isEqualTo("http.endpoint");
    assertThat(attrs.get(AttributeKey.stringKey("audit.source.id"))).isEqualTo("10.0.0.1");
    assertThat(attrs.get(AttributeKey.stringKey("audit.source.type"))).isEqualTo("ipv4");
    assertThat(attrs.get(AttributeKey.longKey("audit.sequence.number"))).isEqualTo(42L);
    assertThat(attrs.get(AttributeKey.stringKey("audit.prev.hash"))).isEqualTo("prevhash123");
    assertThat(attrs.get(AttributeKey.stringKey("audit.schema.version"))).isEqualTo("1.0.0");
  }

  @Test
  void integrityValue_base64Encoded() {
    byte[] proof = new byte[] {0x01, 0x02, 0x03};
    AuditRecordData data =
        SdkAuditRecordData.create(
            Resource.getDefault(), "test", null, null,
            "id4", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "signed.event", "svc", ActorType.SERVICE,
            "SIGN", Outcome.SUCCESS,
            null, null, null, null, null, Attributes.empty(), proof, 0, null, null);
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    String encoded = adapted.getAttributes().get(AttributeKey.stringKey("audit.integrity.value"));
    assertThat(encoded).isEqualTo(Base64.getEncoder().encodeToString(proof));
  }

  @Test
  void instrumentationScope_isEmpty() {
    LogRecordData adapted = new AuditLogRecordDataAdapter(buildMinimal());
    assertThat(adapted.getInstrumentationScopeInfo().getName()).isEmpty();
  }

  @Test
  void severityNumber_isUndefined() {
    LogRecordData adapted = new AuditLogRecordDataAdapter(buildMinimal());
    assertThat(adapted.getSeverity())
        .isEqualTo(Severity.UNDEFINED_SEVERITY_NUMBER);
    assertThat(adapted.getSeverityText()).isNull();
  }

  @Test
  void timestampsPreserved() {
    LogRecordData adapted = new AuditLogRecordDataAdapter(buildMinimal());
    assertThat(adapted.getTimestampEpochNanos()).isEqualTo(TIMESTAMP_NANOS);
    assertThat(adapted.getObservedTimestampEpochNanos()).isEqualTo(OBSERVED_NANOS);
  }

  @Test
  void eventNamePreserved() {
    LogRecordData adapted = new AuditLogRecordDataAdapter(buildMinimal());
    assertThat(adapted.getEventName()).isEqualTo("user.login.success");
  }

  @Test
  void resourcePreserved() {
    Resource resource = Resource.builder().put("service.name", "auth-svc").build();
    AuditRecordData data =
        SdkAuditRecordData.create(
            resource, "test", null, null,
            "id5", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "user.login.success", "u1", ActorType.USER,
            "LOGIN", Outcome.SUCCESS,
            null, null, null, null, null, Attributes.empty(), null, 0, null, null);
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    assertThat(adapted.getResource()).isEqualTo(resource);
  }

  @Test
  void userAttributes_mergedIntoAdaptedAttributes() {
    Attributes userAttrs = Attributes.of(AttributeKey.stringKey("custom.key"), "custom-value");
    AuditRecordData data =
        SdkAuditRecordData.create(
            Resource.getDefault(), "test", null, null,
            "id6", TIMESTAMP_NANOS, OBSERVED_NANOS,
            "custom.event", "u1", ActorType.USER,
            "READ", Outcome.SUCCESS,
            null, null, null, null, null, userAttrs, null, 0, null, null);
    LogRecordData adapted = new AuditLogRecordDataAdapter(data);
    assertThat(adapted.getAttributes().get(AttributeKey.stringKey("custom.key")))
        .isEqualTo("custom-value");
  }

  private static AuditRecordData buildMinimal() {
    return SdkAuditRecordData.create(
        Resource.getDefault(),
        "test-logger", null, null,
        "test-record-id",
        TIMESTAMP_NANOS, OBSERVED_NANOS,
        "user.login.success",
        "u8472", ActorType.USER,
        "LOGIN", Outcome.SUCCESS,
        null, null, null, null, null,
        Attributes.empty(), null, 0, null, null);
  }
}
