package com.teleflow.billing_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeRequest {

    @NotBlank(message = "trackingId is required")
    private String trackingId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "planName is required")
    private String planName;

    @NotNull(message = "amount is required")
    private BigDecimal amount;
}
