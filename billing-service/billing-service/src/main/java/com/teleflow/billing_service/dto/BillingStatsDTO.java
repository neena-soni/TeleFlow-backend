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
public class BillingStatsDTO {
    private long totalTransactions;
    private long successfulCharges;
    private long refundedCharges;
    private BigDecimal totalRevenueCollected;
    private boolean chaosModeActive;
    private String status;
}
