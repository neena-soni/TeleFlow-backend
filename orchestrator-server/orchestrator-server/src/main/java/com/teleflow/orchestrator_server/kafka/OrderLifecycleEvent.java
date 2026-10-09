package com.teleflow.orchestrator_server.kafka;

import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Typed Kafka message published to the 'teleflow.orders.lifecycle' topic.
 *
 * Published on every order state transition:
 *   - When a step starts (RUNNING)
 *   - When a step succeeds (SUCCESS)
 *   - When a step fails / retries (FAILED, RETRYING)
 *   - When compensation runs (ROLLED_BACK)
 *   - When order completes or fails (COMPLETED / FAILED)
 *
 * This creates an immutable event log useful for:
 *   - Analytics dashboards
 *   - Audit compliance
 *   - Dead Letter Queue (DLQ) replay
 *   - Future consumers (e.g., a metrics aggregator service)
 *
 * Serialized as JSON by JsonSerializer (configured in application.properties).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLifecycleEvent {

    /** UUID tracking ID — correlates events across services. */
    private String trackingId;

    /** Database primary key of the TelecomOrder. */
    private Long orderId;

    /** Customer identifier. */
    private String customerId;

    /** Which step this event describes. Null if it's an order-level event. */
    private StepName stepName;

    /** Status of the step at time of this event. */
    private StepStatus stepStatus;

    /** Overall order status at time of this event. */
    private OrderStatus orderStatus;

    /** Failure message if step or order failed. */
    private String reason;

    /** Retry attempt number (0 if not a retry event). */
    private int retryCount;

    /** ISO-8601 timestamp of the event. */
    @Builder.Default
    private String eventTimestamp = Instant.now().toString();
}
