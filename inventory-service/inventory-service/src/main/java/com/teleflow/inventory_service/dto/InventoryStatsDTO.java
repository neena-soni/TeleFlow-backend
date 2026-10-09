package com.teleflow.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatsDTO {
    private long totalReservations;
    private long activeAllocations;
    private long releasedAllocations;
    private int poolCapacity;
    private int availableCapacity;
    private String status;
}
