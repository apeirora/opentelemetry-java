/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.Nullable;

/** Immutable AutoValue implementation of {@link AuditRecordData}. */
@AutoValue
@SuppressWarnings("AutoValueMutable")
public abstract class SdkAuditRecordData implements AuditRecordData {

  SdkAuditRecordData() {}

  /** Creates a new {@link SdkAuditRecordData}. */
  @SuppressWarnings("TooManyParameters")
  public static SdkAuditRecordData create(
      Resource resource,
      String loggerName,
      @Nullable String loggerVersion,
      @Nullable String schemaUrl,
      String recordId,
      long timestampEpochNanos,
      long observedTimestampEpochNanos,
      String eventName,
      String actorId,
      ActorType actorType,
      String action,
      Outcome outcome,
      @Nullable String targetId,
      @Nullable String targetType,
      @Nullable String sourceId,
      @Nullable String sourceType,
      @Nullable Value<?> body,
      Attributes attributes,
      @Nullable byte[] signature,
      @Nullable String algorithm,
      @Nullable byte[] certificate,
      @Nullable byte[] hmac,
      @Nullable String hmacAlgorithm,
      long sequenceNo,
      @Nullable String prevHash,
      @Nullable String schemaVersion) {
    return new AutoValue_SdkAuditRecordData(
        resource,
        loggerName,
        loggerVersion,
        schemaUrl,
        recordId,
        timestampEpochNanos,
        observedTimestampEpochNanos,
        eventName,
        actorId,
        actorType,
        action,
        outcome,
        targetId,
        targetType,
        sourceId,
        sourceType,
        body,
        attributes,
        signature,
        algorithm,
        certificate,
        hmac,
        hmacAlgorithm,
        sequenceNo,
        prevHash,
        schemaVersion);
  }
}
