/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.audit;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.audit.ActorType;
import io.opentelemetry.api.audit.Outcome;
import io.opentelemetry.api.common.Attributes;
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
      long timestampEpochNanos,
      long observedTimestampEpochNanos,
      Attributes attributes,
      String eventName,
      String loggerName,
      @Nullable String loggerVersion,
      @Nullable String schemaUrl,
      String recordId,
      String actorId,
      ActorType actorType,
      String action,
      Outcome outcome,
      @Nullable String targetId,
      @Nullable String targetType,
      @Nullable String sourceId,
      @Nullable String sourceType,
      @Nullable byte[] integrityValue,
      long sequenceNo,
      @Nullable String prevHash,
      @Nullable String schemaVersion) {
    return new AutoValue_SdkAuditRecordData(
        resource,
        timestampEpochNanos,
        observedTimestampEpochNanos,
        attributes,
        eventName,
        loggerName,
        loggerVersion,
        schemaUrl,
        recordId,
        actorId,
        actorType,
        action,
        outcome,
        targetId,
        targetType,
        sourceId,
        sourceType,
        integrityValue,
        sequenceNo,
        prevHash,
        schemaVersion);
  }
}
