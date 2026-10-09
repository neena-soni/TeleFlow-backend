package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body received from inventory-service POST /api/inventory/reserve.
 *
 * portId and routerId are passed downstream to the network-service
 * so it knows which physical port to configure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveResponse {
    private String trackingId;
    private String portId;       // e.g. "PORT-BLR-045"
    private String routerId;     // e.g. "RTR-JIO-982"
    private String status;       // "ALLOCATED"
}
