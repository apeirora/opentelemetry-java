/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global singleton holder for the process-wide {@link AuditProvider}.
 *
 * <p>In most applications there is only one {@link AuditProvider}. {@link #set(AuditProvider)}
 * SHOULD be called once, early in the application lifecycle (for example, in the same place where
 * the OpenTelemetry SDK is initialised).
 */
public final class GlobalAuditProvider {

  private static final AtomicReference<AuditProvider> globalProvider = new AtomicReference<>();

  private GlobalAuditProvider() {}

  /**
   * Returns the globally registered {@link AuditProvider}, or throws {@link NullPointerException}
   * if {@link #set(AuditProvider)} was not called before.
   */
  public static AuditProvider get() {
    return Objects.requireNonNull(globalProvider.get());
  }

  /**
   * Sets the globally registered {@link AuditProvider}.
   *
   * @param auditProvider the provider to register; MUST NOT be null
   * @throws IllegalArgumentException if {@code auditProvider} is null
   */
  public static void set(AuditProvider auditProvider) {
    globalProvider.set(auditProvider);
  }
}
