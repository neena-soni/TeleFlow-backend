package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body sent to inventory-service POST /api/inventory/reserve.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveRequest {
    private String trackingId;
    private String customerId;
    private String planName;
}
