/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logs.export;

import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.IOException;
import java.util.Collection;

public interface AuditLogStore {

  void save(LogRecordData logRecord) throws IOException;

  void removeAll(Collection<LogRecordData> logs);

  Collection<LogRecordData> getAll();

  /**
   * Delegates to size of {@link #getAll()}.
   *
   * @return Returns the number of log records in the store.
   */
  default int size() {
    return getAll().size();
  }

  /**
   * Delegates to {@link #size()} == 0.
   *
   * @return Returns true if the store contains no log records.
   */
  default boolean isEmpty() {
    return size() == 0;
  }
}
