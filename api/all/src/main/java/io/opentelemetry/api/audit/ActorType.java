/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.api.audit;

/**
 * Classifies the kind of entity that performed an auditable action.
 *
 * @see AuditRecordBuilder#setActor(String, ActorType)
 */
public enum ActorType {

  /** A human user, identified by a user account. */
  USER,

  /** An automated service, daemon, or service account. */
  SERVICE,

  /** The operating system or a privileged system component. */
  SYSTEM;
}
