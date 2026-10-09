package com.teleflow.orchestrator_server.model;

import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root for a telecom service activation order.
 *
 * Hibernate auto-creates the 'telecom_orders' table via ddl-auto=update.
 *
 * Relationships:
 *   One TelecomOrder → Many OrderStepExecution (audit trail per step)
 */
@Entity
@Table(name = "telecom_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelecomOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID used as Temporal workflowId and Kafka message key.
     * Enables distributed tracing across all services.
     */
    @Column(name = "tracking_id", unique = true, nullable = false, length = 36)
    private String trackingId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_email")
    private String customerEmail;

    /**
     * Plan selected by customer. E.g.: FIBER_300MBPS, 5G_UNLIMITED
     */
    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    /**
     * Current lifecycle status of this order.
     * @see OrderStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OrderStatus status = OrderStatus.RECEIVED;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Set when the order reaches COMPLETED or FAILED status. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Total milliseconds from RECEIVED to COMPLETED or FAILED.
     * Used for dashboard metrics: average activation time.
     */
    @Column(name = "activation_duration_ms")
    private Long activationDurationMs;

    /**
     * Reason for failure (captured from Temporal ActivityFailure message).
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Ordered step audit trail.
     * Cascade ALL + orphanRemoval means steps are managed entirely through the order.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startedAt ASC")
    @Builder.Default
    private List<OrderStepExecution> steps = new ArrayList<>();
}
