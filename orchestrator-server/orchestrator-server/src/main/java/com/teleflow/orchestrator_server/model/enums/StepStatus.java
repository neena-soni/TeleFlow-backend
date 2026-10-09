package com.teleflow.orchestrator_server.model.enums;

/**
 * Lifecycle states of a single OrderStepExecution.
 *
 * Forward path:  PENDING → RUNNING → SUCCESS
 * Failure path:  RUNNING → RETRYING → FAILED
 * Rollback path: SUCCESS → ROLLED_BACK
 */
public enum StepStatus {

    /** Step created in DB; not yet started. */
    PENDING,

    /** Temporal activity has been dispatched; awaiting response. */
    RUNNING,

    /** Downstream service call succeeded. */
    SUCCESS,

    /** Activity failed; Temporal is retrying. */
    RETRYING,

    /** All retries exhausted; step has permanently failed. */
    FAILED,

    /** Compensation for this step completed successfully. */
    ROLLED_BACK
}
