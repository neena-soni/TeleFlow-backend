package com.teleflow.orchestrator_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Inbound request body for POST /api/orders/activate.
 *
 * Example JSON:
 * {
 *   "customerId": "CUST-RAHUL-001",
 *   "planName": "FIBER_300MBPS",
 *   "customerEmail": "rahul@example.com"
 * }
 */
@Data
public class ActivationRequestDTO {

    @NotBlank(message = "customerId is required")
    private String customerId;

    /**
     * Supported plans: FIBER_100MBPS, FIBER_300MBPS, 5G_UNLIMITED, 5G_BASIC
     */
    @NotBlank(message = "planName is required")
    @Pattern(
        regexp = "FIBER_100MBPS|FIBER_300MBPS|5G_UNLIMITED|5G_BASIC",
        message = "planName must be one of: FIBER_100MBPS, FIBER_300MBPS, 5G_UNLIMITED, 5G_BASIC"
    )
    private String planName;

    @Email(message = "customerEmail must be a valid email address")
    private String customerEmail;
}
