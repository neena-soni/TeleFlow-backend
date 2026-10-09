package com.teleflow.orchestrator_server.temporal.workflow;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.downstream.InventoryReserveResponse;
import com.teleflow.orchestrator_server.dto.downstream.NetworkActivateResponse;
import com.teleflow.orchestrator_server.temporal.activity.BillingActivity;
import com.teleflow.orchestrator_server.temporal.activity.InventoryActivity;
import com.teleflow.orchestrator_server.temporal.activity.NetworkActivity;
import com.teleflow.orchestrator_server.temporal.activity.NotificationActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * ★ THE SAGA BRAIN ★ — Core orchestration logic for TeleFlow.
 *
 * This class contains the entire saga state machine. It is instantiated
 * fresh per workflow execution by Temporal (NOT a Spring singleton).
 * Dependencies are obtained via activity stubs (not Spring injection).
 *
 * ── Execution sequence (happy path) ─────────────────────────────────────
 *   1. reserve inventory    (InventoryActivity.reserve)
 *   2. activate network     (NetworkActivity.activate)     ← uses portId from step 1
 *   3. charge billing       (BillingActivity.charge)
 *   4. send notification    (NotificationActivity.sendSuccess)
 *
 * ── Compensation sequence (on any failure) ───────────────────────────────
 * Uses Temporal's Saga.Builder to register compensations in order.
 * On failure, Saga.compensate() runs them in REVERSE order:
 *   refund billing → deactivate network → release inventory → sendFailure
 *
 * ── Retry Policy ─────────────────────────────────────────────────────────
 *   maxAttempts:          3  (visible as Retry 1/3, 2/3, 3/3 in UI)
 *   initialInterval:      2s (first retry after 2 seconds)
 *   backoffCoefficient:   2.0 (next: 4s, then 8s — exponential backoff)
 *   startToCloseTimeout:  30s per activity
 */
@Slf4j
public class ServiceActivationWorkflowImpl implements ServiceActivationWorkflow {

    // ─── Retry policy shared across all activities ───────────────────────────
    private static final RetryOptions STANDARD_RETRY = RetryOptions.newBuilder()
            .setMaximumAttempts(3)
            .setInitialInterval(Duration.ofSeconds(2))
            .setBackoffCoefficient(2.0)
            .setMaximumInterval(Duration.ofSeconds(30))
            .build();

    // ─── Activity options (timeout + retry) ──────────────────────────────────
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setScheduleToStartTimeout(Duration.ofSeconds(10))
            .setRetryOptions(STANDARD_RETRY)
            .build();

    // Best-effort for notifications — fewer retries, still has a timeout
    private static final ActivityOptions NOTIFICATION_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(15))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build())
            .build();

    // ─── Activity stubs (Temporal proxies) ───────────────────────────────────
    // These are NOT real beans — they are dynamic proxies created by Temporal.
    // When called, Temporal schedules the activity on the task queue.
    private final InventoryActivity inventoryActivity =
            Workflow.newActivityStub(InventoryActivity.class, ACTIVITY_OPTIONS);

    private final NetworkActivity networkActivity =
            Workflow.newActivityStub(NetworkActivity.class, ACTIVITY_OPTIONS);

    private final BillingActivity billingActivity =
            Workflow.newActivityStub(BillingActivity.class, ACTIVITY_OPTIONS);

    private final NotificationActivity notificationActivity =
            Workflow.newActivityStub(NotificationActivity.class, NOTIFICATION_OPTIONS);

    // ─── Main workflow method ─────────────────────────────────────────────────

    @Override
    public void activate(String trackingId, ActivationRequestDTO request) {
        log.info("[WORKFLOW] Starting activation: trackingId={} plan={}",
                trackingId, request.getPlanName());

        /*
         * Temporal Saga Builder — manages compensation registrations.
         *
         * Pattern:
         *   saga.addCompensation(activity::compensationMethod, args)
         *   activity.forwardMethod(args)
         *
         * The compensation is registered BEFORE the forward call.
         * If the forward call fails, Saga.compensate() runs all registered
         * compensations in reverse order.
         *
         * parallelCompensation=false ensures sequential rollback (safe for demo).
         */
        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build());

        try {
            // ────────────────────────────────────────────────────────────────
            // STEP 1: INVENTORY
            // ────────────────────────────────────────────────────────────────
            log.info("[WORKFLOW] Executing INVENTORY step: trackingId={}", trackingId);

            // Register compensation BEFORE the forward action
            saga.addCompensation(inventoryActivity::release, trackingId);

            InventoryReserveResponse inventoryResult = inventoryActivity.reserve(trackingId, request);

            String portId   = inventoryResult != null ? inventoryResult.getPortId()   : "PORT-DEFAULT";
            String routerId = inventoryResult != null ? inventoryResult.getRouterId() : "RTR-DEFAULT";

            log.info("[WORKFLOW] INVENTORY done: portId={} routerId={}", portId, routerId);

            // ────────────────────────────────────────────────────────────────
            // STEP 2: NETWORK
            // ────────────────────────────────────────────────────────────────
            log.info("[WORKFLOW] Executing NETWORK step: trackingId={}", trackingId);

            saga.addCompensation(networkActivity::deactivate, trackingId);

            NetworkActivateResponse networkResult = networkActivity.activate(
                    trackingId, request, portId, routerId);

            log.info("[WORKFLOW] NETWORK done: vlan={} bandwidth={}",
                    networkResult != null ? networkResult.getVlanTag() : null,
                    networkResult != null ? networkResult.getAllocatedBandwidthMbps() : null);

            // ────────────────────────────────────────────────────────────────
            // STEP 3: BILLING  ← chaos switch can trigger failure here
            // ────────────────────────────────────────────────────────────────
            log.info("[WORKFLOW] Executing BILLING step: trackingId={}", trackingId);

            saga.addCompensation(billingActivity::refund, trackingId);

            billingActivity.charge(trackingId, request);

            log.info("[WORKFLOW] BILLING done: trackingId={}", trackingId);

            // ────────────────────────────────────────────────────────────────
            // STEP 4: NOTIFICATION (success)
            // ────────────────────────────────────────────────────────────────
            log.info("[WORKFLOW] Sending SUCCESS notification: trackingId={}", trackingId);

            notificationActivity.sendSuccess(
                    trackingId,
                    request.getCustomerEmail(),
                    request.getPlanName());

            log.info("[WORKFLOW] ✅ Activation COMPLETED: trackingId={}", trackingId);

        } catch (ActivityFailure ex) {
            /*
             * One of the activities failed all retries.
             * Temporal wraps it in ActivityFailure with the cause inside.
             *
             * We trigger Saga.compensate() which runs all registered
             * compensations in reverse order:
             *   - If billing registered: refund (no-op if billing never charged)
             *   - If network registered: deactivate
             *   - If inventory registered: release
             */
            String failedStep = ex.getActivityType() != null ? ex.getActivityType() : "UNKNOWN";
            String reason = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

            log.error("[WORKFLOW] ❌ Activity FAILED: step={} trackingId={} reason={}",
                    failedStep, trackingId, reason);

            // Run all registered compensations in reverse
            saga.compensate();

            // Send failure notification (best-effort — won't throw)
            try {
                notificationActivity.sendFailure(
                        trackingId,
                        request.getCustomerEmail(),
                        failedStep,
                        reason);
            } catch (Exception notifEx) {
                log.warn("[WORKFLOW] Failure notification itself failed: {}", notifEx.getMessage());
            }

            // Re-throw so Temporal marks this workflow execution as FAILED
            // The Temporal Web UI will show the full failure timeline.
            throw Workflow.wrap(ex);
        }
    }
}
