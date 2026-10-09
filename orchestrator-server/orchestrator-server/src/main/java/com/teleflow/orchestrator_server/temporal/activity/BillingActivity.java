package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.downstream.BillingChargeResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity Interface for Billing operations.
 */
@ActivityInterface
public interface BillingActivity {

    /**
     * Charge the customer and create a billing subscription.
     * Called in the FORWARD path after network is provisioned.
     *
     * @param trackingId UUID of the order
     * @param request    order details (customerId, planName)
     * @return billing account details
     */
    @ActivityMethod
    BillingChargeResponse charge(String trackingId, ActivationRequestDTO request);

    /**
     * Refund the customer and cancel the billing subscription.
     * Called in the COMPENSATION (rollback) path.
     *
     * @param trackingId UUID of the order
     */
    @ActivityMethod
    void refund(String trackingId);
}
