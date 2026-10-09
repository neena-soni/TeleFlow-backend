package com.teleflow.orchestrator_server.model;

import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit record for a single step in the activation saga.
 *
 * One row per step per order. There will be exactly 4 rows per order:
 *   INVENTORY, NETWORK, BILLING, NOTIFICATION
 *
 * Hibernate auto-creates the 'order_step_executions' table.
 */
@Entity
@Table(name = "order_step_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent order — many-to-one back-reference.
     * FK column: order_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private TelecomOrder order;

    /**
     * Which saga step this row represents.
     * @see StepName
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", nullable = false, length = 50)
    private StepName stepName;

    /**
     * Current status of this step.
     * @see StepStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_status", nullable = false, length = 50)
    @Builder.Default
    private StepStatus stepStatus = StepStatus.PENDING;

    /**
     * How many times Temporal has retried this step.
     * Incremented by OrderStatusService.markStepRetrying().
     */
    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    /**
     * Error description if the step failed.
     * Populated from the ActivityFailure message or HTTP error body.
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /** Timestamp when this step transitioned to RUNNING. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Timestamp when this step reached a terminal state (SUCCESS, FAILED, ROLLED_BACK). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
