/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

/**
 * The result of an auditable action.
 *
 * @see AuditRecordBuilder#setOutcome(Outcome)
 */
public enum Outcome {

  /** The action completed successfully. */
  SUCCESS,

  /** The action was attempted but did not complete successfully. */
  FAILURE,

  /** The outcome could not be determined at the time of emission. */
  UNKNOWN
}
