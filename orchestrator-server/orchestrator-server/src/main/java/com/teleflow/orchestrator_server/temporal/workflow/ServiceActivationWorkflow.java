package com.teleflow.orchestrator_server.temporal.workflow;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal Workflow Interface for telecom service activation.
 *
 * ── What is a Temporal Workflow? ─────────────────────────────────────────
 * A Workflow is a durable, fault-tolerant orchestration function.
 * Its execution state is persisted to Temporal's database at every step.
 *
 * Key properties:
 *  - If the server crashes mid-workflow, Temporal replays the history
 *    and resumes exactly where it left off.
 *  - Workflow code must be deterministic (no random, no System.currentTimeMillis).
 *  - All side-effects must go inside Activities (HTTP calls, DB writes, etc.).
 *
 * ── How the Workflow is started ──────────────────────────────────────────
 * OrderOrchestrationService creates a WorkflowStub and calls activate() async.
 * Temporal assigns it to the "teleflow-task-queue" where our Worker picks it up.
 *
 * The workflowId = trackingId (UUID) — this ensures only ONE execution
 * per order exists (idempotent start).
 */
@WorkflowInterface
public interface ServiceActivationWorkflow {

    /**
     * Orchestrates the full service activation saga.
     *
     * @param trackingId UUID of the order (also the Temporal workflowId)
     * @param request    the original activation request
     */
    @WorkflowMethod
    void activate(String trackingId, ActivationRequestDTO request);
}
