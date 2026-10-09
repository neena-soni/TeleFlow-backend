package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.downstream.InventoryReserveResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity Interface for Inventory operations.
 *
 * ── What is a Temporal Activity? ─────────────────────────────────────────
 * An Activity is a regular Java method that performs a single unit of work
 * (usually a side-effect: HTTP call, DB write, email send).
 *
 * Temporal manages retries, timeouts, and heartbeating for activities.
 * If the worker crashes mid-activity, Temporal reschedules it on another worker.
 *
 * ── Interface vs Implementation ──────────────────────────────────────────
 * The @ActivityInterface is the contract that the Workflow uses to create
 * an activity stub. The Workflow NEVER directly instantiates the implementation.
 * Temporal injects a dynamic proxy that records the activity invocation.
 */
@ActivityInterface
public interface InventoryActivity {

    /**
     * Reserve a physical port/SIM/router for the given order.
     * Called in the FORWARD path of the saga.
     *
     * @param trackingId UUID of the order
     * @param request    order details (customerId, planName)
     * @return reservation details (portId, routerId) passed to the next step
     */
    @ActivityMethod
    InventoryReserveResponse reserve(String trackingId, ActivationRequestDTO request);

    /**
     * Release a previously reserved port/SIM/router.
     * Called in the COMPENSATION (rollback) path of the saga.
     *
     * @param trackingId UUID of the order to release resources for
     */
    @ActivityMethod
    void release(String trackingId);
}
