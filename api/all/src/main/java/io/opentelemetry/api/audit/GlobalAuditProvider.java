/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Global singleton holder for the process-wide {@link AuditProvider}.
 *
 * <p>In most applications there is only one {@link AuditProvider}. {@link #set(AuditProvider)}
 * SHOULD be called once, early in the application lifecycle (for example, in the same place where
 * the OpenTelemetry SDK is initialised).
 *
 * <p>If no provider is registered, {@link #get()} returns the no-op provider from {@link
 * AuditProvider#noop()}.
 */
public final class GlobalAuditProvider {

  private static final AtomicReference<AuditProvider> globalProvider =
      new AtomicReference<>(AuditProvider.noop());

  private GlobalAuditProvider() {}

  /** Returns the globally registered {@link AuditProvider}, or the no-op instance if none set. */
  public static AuditProvider get() {
    return globalProvider.get();
  }

  /**
   * Sets the globally registered {@link AuditProvider}.
   *
   * @param auditProvider the provider to register; MUST NOT be null
   * @throws IllegalArgumentException if {@code auditProvider} is null
   */
  public static void set(AuditProvider auditProvider) {
    if (auditProvider == null) {
      throw new IllegalArgumentException("auditProvider must not be null");
    }
    globalProvider.set(auditProvider);
  }

  /** Resets the global provider to the no-op implementation. Intended for use in tests only. */
  public static void resetForTest() {
    globalProvider.set(AuditProvider.noop());
  }
}
