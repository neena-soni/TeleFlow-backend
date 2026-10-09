package com.teleflow.network_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkActivateRequest {

    @NotBlank(message = "trackingId is required")
    private String trackingId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "planName is required")
    private String planName;

    private String portId;
    private String routerId;
}
