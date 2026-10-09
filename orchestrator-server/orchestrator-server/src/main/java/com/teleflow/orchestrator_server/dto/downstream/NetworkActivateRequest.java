package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body sent to network-service POST /api/network/activate.
 *
 * portId comes from the InventoryReserveResponse — this is how the
 * workflow chains data between saga steps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkActivateRequest {
    private String trackingId;
    private String customerId;
    private String planName;
    private String portId;    // from inventory step
    private String routerId;  // from inventory step
}
