package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body sent to billing-service POST /api/billing/charge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeRequest {
    private String trackingId;
    private String customerId;
    private String planName;
    private BigDecimal amount;   // derived from plan in BillingActivityImpl
}
