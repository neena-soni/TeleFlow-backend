package com.teleflow.orchestrator_server.dto;

import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * WebSocket push payload.
 *
 * Sent to /topic/order-events via SimpMessagingTemplate on every
 * state transition in the saga (step starts, step succeeds, step fails,
 * rollback, order completed, order failed).
 *
 * The React/Angular dashboard subscribes to this topic and re-renders
 * the order card in real time.
 *
 * Example JSON pushed to WebSocket:
 * {
 *   "orderId": 1,
 *   "trackingId": "TRK-8821",
 *   "customerId": "CUST-RAHUL-001",
 *   "currentStep": "BILLING",
 *   "stepStatus": "FAILED",
 *   "orderStatus": "COMPENSATING",
 *   "retryCount": 3,
 *   "message": "Billing failed after 3 retries. Starting rollback.",
 *   "timestamp": "2024-10-09T02:30:00Z"
 * }
 */
@Data
@Builder
public class OrderStatusUpdateDTO {

    private Long orderId;
    private String trackingId;
    private String customerId;
    private String planName;

    /** The step that just transitioned. Null for order-level events (COMPLETED/FAILED). */
    private StepName currentStep;

    /** Status of that step. */
    private StepStatus stepStatus;

    /** Overall order status. */
    private OrderStatus orderStatus;

    /** Retry count for the current step (useful for showing "Retry 2/3"). */
    private int retryCount;

    /** Human-readable message for display on the dashboard. */
    private String message;

    /** ISO-8601 UTC timestamp. */
    @Builder.Default
    private String timestamp = Instant.now().toString();
}
