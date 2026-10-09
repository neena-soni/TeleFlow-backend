package com.teleflow.network_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkActivateResponse {
    private String trackingId;
    private String vlanTag;
    private Integer allocatedBandwidthMbps;
    private String status;
}
