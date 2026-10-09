package com.teleflow.network_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkStatsDTO {
    private long totalProvisions;
    private long activeLines;
    private long deactivatedLines;
    private long totalAllocatedBandwidthMbps;
    private String status;
}
