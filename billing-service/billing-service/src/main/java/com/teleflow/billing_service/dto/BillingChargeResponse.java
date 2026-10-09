package com.teleflow.billing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeResponse {
    private String trackingId;
    private String paymentId;
    private BigDecimal amount;
    private String status;
}
