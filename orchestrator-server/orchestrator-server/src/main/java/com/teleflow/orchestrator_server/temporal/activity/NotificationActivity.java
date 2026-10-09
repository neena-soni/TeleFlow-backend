package com.teleflow.orchestrator_server.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity Interface for Notification operations.
 *
 * Notification is a "best-effort" activity — it runs after the saga
 * has completed (either success or failure). It does NOT participate
 * in compensation (no rollback needed for a sent notification).
 */
@ActivityInterface
public interface NotificationActivity {

    /**
     * Send a success confirmation to the customer.
     * Called after all forward saga steps succeed.
     *
     * @param trackingId    UUID of the order
     * @param customerEmail recipient email address
     * @param planName      activated plan name
     */
    @ActivityMethod
    void sendSuccess(String trackingId, String customerEmail, String planName);

    /**
     * Send a failure/rollback explanation to the customer.
     * Called after the saga compensation completes.
     *
     * @param trackingId    UUID of the order
     * @param customerEmail recipient email address
     * @param failedStep    which step caused the failure
     * @param reason        human-readable failure reason
     */
    @ActivityMethod
    void sendFailure(String trackingId, String customerEmail, String failedStep, String reason);
}
