package com.teleflow.orchestrator_server.dto.downstream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response body received from billing-service POST /api/billing/charge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeResponse {
    private String trackingId;
    private String billingAccountId; // e.g. "BILL-ACC-9921"
    private BigDecimal amountCharged;
    private String state;            // "CHARGED"
}
