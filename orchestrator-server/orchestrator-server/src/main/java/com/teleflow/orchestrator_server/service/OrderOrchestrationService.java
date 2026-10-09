package com.teleflow.orchestrator_server.service;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.OrderResponseDTO;
import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import com.teleflow.orchestrator_server.kafka.OrderLifecycleEvent;
import com.teleflow.orchestrator_server.model.TelecomOrder;
import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.temporal.workflow.ServiceActivationWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Orchestration service — the bridge between the REST controller and Temporal.
 *
 * Responsibilities:
 *   1. Generate a unique trackingId (UUID) for the order
 *   2. Persist the order in PostgreSQL (with 4 PENDING steps)
 *   3. Start the Temporal workflow asynchronously
 *   4. Publish the initial Kafka "ORDER_RECEIVED" event
 *   5. Broadcast the initial WebSocket "order started" update
 *
 * ── Why async workflow start? ─────────────────────────────────────────────
 * WorkflowClient.execute() is asynchronous — it sends a "StartWorkflow"
 * command to the Temporal server and returns immediately. The actual
 * workflow execution happens on the Worker thread pool.
 *
 * This means POST /api/orders/activate returns ~immediately (fast REST response)
 * while the saga runs in the background. The dashboard receives live
 * updates via WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {

    @Value("${temporal.task-queue}")
    private String taskQueue;

    private final WorkflowClient workflowClient;
    private final OrderStatusService orderStatusService;
    private final SagaEventPublisher sagaEventPublisher;
    private final DashboardBroadcastService dashboardBroadcastService;

    /**
     * Entry point for a new activation order.
     *
     * @param request validated activation request from the REST controller
     * @return the persisted order DTO (status=RECEIVED, steps=PENDING)
     */
    public OrderResponseDTO startActivation(ActivationRequestDTO request) {
        // ① Generate tracking UUID
        String trackingId = UUID.randomUUID().toString();
        log.info("[ORCHESTRATION] New order: customerId={} plan={} trackingId={}",
                request.getCustomerId(), request.getPlanName(), trackingId);

        // ② Persist order + 4 PENDING steps in PostgreSQL
        TelecomOrder order = TelecomOrder.builder()
                .trackingId(trackingId)
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .planName(request.getPlanName())
                .status(OrderStatus.RECEIVED)
                .build();

        TelecomOrder saved = orderStatusService.createOrderWithSteps(order);

        // ③ Start Temporal workflow asynchronously
        // workflowId = trackingId ensures idempotent start
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(trackingId)          // idempotent: prevents duplicate runs
                .setTaskQueue(taskQueue)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                .build();

        ServiceActivationWorkflow workflow =
                workflowClient.newWorkflowStub(ServiceActivationWorkflow.class, options);

        // Fire-and-forget: workflow runs on the Worker (background)
        WorkflowClient.start(workflow::activate, trackingId, request);

        // Mark order as IN_PROGRESS now that workflow has been dispatched
        orderStatusService.markOrderInProgress(trackingId);

        log.info("[ORCHESTRATION] Temporal workflow started: workflowId={}", trackingId);

        // ④ Publish initial Kafka event
        sagaEventPublisher.publish(OrderLifecycleEvent.builder()
                .trackingId(trackingId)
                .orderId(saved.getId())
                .customerId(request.getCustomerId())
                .orderStatus(OrderStatus.IN_PROGRESS)
                .build());

        // ⑤ Broadcast initial WebSocket update
        dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                .orderId(saved.getId())
                .trackingId(trackingId)
                .customerId(request.getCustomerId())
                .planName(request.getPlanName())
                .orderStatus(OrderStatus.IN_PROGRESS)
                .message("🚀 Order received! Starting activation saga...")
                .build());

        return orderStatusService.getOrderById(saved.getId());
    }
}
