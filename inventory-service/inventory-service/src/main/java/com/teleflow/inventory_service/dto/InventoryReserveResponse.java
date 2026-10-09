package com.teleflow.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveResponse {
    private String trackingId;
    private String portId;
    private String routerId;
    private String status;
}
