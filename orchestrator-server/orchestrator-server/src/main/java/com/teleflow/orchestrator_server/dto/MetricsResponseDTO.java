package com.teleflow.orchestrator_server.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregated dashboard metrics returned by GET /api/orders/metrics.
 *
 * Used by the operator dashboard to show KPIs.
 */
@Data
@Builder
public class MetricsResponseDTO {

    /** Total number of orders ever submitted. */
    private long totalOrders;

    /** Orders that reached COMPLETED status. */
    private long successCount;

    /** Orders that reached FAILED status. */
    private long failedCount;

    /** Orders currently IN_PROGRESS or COMPENSATING. */
    private long inProgressCount;

    /** (successCount / totalOrders) * 100, rounded to 2 decimal places. */
    private double successRatePercent;

    /** Average time in milliseconds from order creation to COMPLETED. Null if no completed orders. */
    private Double avgActivationDurationMs;
}
