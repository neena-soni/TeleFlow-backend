package com.teleflow.orchestrator_server.model.enums;

/**
 * Lifecycle states of a TelecomOrder.
 *
 * State machine:
 *   RECEIVED → IN_PROGRESS → COMPLETED
 *                          → COMPENSATING → FAILED
 */
public enum OrderStatus {

    /** Order received and persisted; Temporal workflow not yet started. */
    RECEIVED,

    /** Temporal workflow is running; activities are executing. */
    IN_PROGRESS,

    /** All activities succeeded; service is live. */
    COMPLETED,

    /** One activity failed; compensation (rollback) is now running. */
    COMPENSATING,

    /** Compensation completed; order is fully rolled back. */
    FAILED
}
