package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body received from network-service POST /api/network/activate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkActivateResponse {
    private String trackingId;
    private String vlanTag;              // e.g. "VLAN-402"
    private Integer allocatedBandwidthMbps; // e.g. 300
    private String status;               // "ACTIVE"
}
