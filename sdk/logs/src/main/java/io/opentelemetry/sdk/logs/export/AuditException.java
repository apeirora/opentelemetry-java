/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

public class AuditException extends RuntimeException {

  private static final long serialVersionUID = 5791873097754062413L;

  @Nullable public Throwable cause;

  @Nullable public Context context;

  @Nullable public ReadWriteLogRecord logRecord;

  @Nullable public Collection<LogRecordData> logRecords;

  public AuditException(Throwable cause, Context context, ReadWriteLogRecord logRecord) {
    super(cause);
    this.logRecord = logRecord;
    this.context = context;
  }

  public AuditException(@Nullable String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  public AuditException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  public AuditException(
      @Nullable String message,
      @Nullable Throwable cause,
      @Nullable Collection<LogRecordData> logs) {
    this(message, cause);
    this.logRecords = logs;
  }

  public void add(@Nullable Collection<LogRecordData> data) {
    if (data == null || data.isEmpty()) {
      return;
    }
    if (logRecords == null) {
      logRecords = new ArrayList<>(data);
    } else {
      logRecords.addAll(data);
    }
  }

  public void because(@Nullable Throwable exception) {
    cause = exception;
  }

  @Override
  @Nullable
  public synchronized Throwable getCause() {
    if (cause != null) {
      return cause;
    }
    return super.getCause();
  }
}
