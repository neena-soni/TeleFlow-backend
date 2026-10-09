package com.teleflow.orchestrator_server.dto;

import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full outbound representation of a TelecomOrder.
 * Returned by GET /api/orders/{id} and POST /api/orders/activate.
 *
 * Includes the full step audit trail for dashboard rendering.
 */
@Data
@Builder
public class OrderResponseDTO {

    private Long id;
    private String trackingId;
    private String customerId;
    private String customerEmail;
    private String planName;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long activationDurationMs;
    private String failureReason;

    /** Ordered list of saga step executions (INVENTORY → NETWORK → BILLING → NOTIFICATION). */
    private List<StepExecutionDTO> steps;
}
