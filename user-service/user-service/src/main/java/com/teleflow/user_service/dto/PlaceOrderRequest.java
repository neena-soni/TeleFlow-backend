package com.teleflow.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PlaceOrderRequest {

    @NotBlank(message = "planName is required")
    @Pattern(
        regexp = "FIBER_100MBPS|FIBER_300MBPS|5G_UNLIMITED|5G_BASIC",
        message = "planName must be one of: FIBER_100MBPS, FIBER_300MBPS, 5G_UNLIMITED, 5G_BASIC"
    )
    private String planName;

    public PlaceOrderRequest() {}

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
}
