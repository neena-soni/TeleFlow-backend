package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.downstream.NetworkActivateResponse;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal Activity Interface for Network provisioning operations.
 */
@ActivityInterface
public interface NetworkActivity {

    /**
     * Provision a VLAN / bandwidth profile on the network.
     * Called in the FORWARD path after inventory is reserved.
     *
     * @param trackingId UUID of the order
     * @param request    order details
     * @param portId     port allocated by inventory step (passed from workflow)
     * @param routerId   router allocated by inventory step
     * @return network profile details (vlanTag, bandwidth)
     */
    @ActivityMethod
    NetworkActivateResponse activate(String trackingId, ActivationRequestDTO request,
                                     String portId, String routerId);

    /**
     * Tear down the provisioned network profile.
     * Called in the COMPENSATION (rollback) path.
     *
     * @param trackingId UUID of the order
     */
    @ActivityMethod
    void deactivate(String trackingId);
}
