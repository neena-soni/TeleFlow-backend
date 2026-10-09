package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import com.teleflow.orchestrator_server.dto.downstream.InventoryReserveRequest;
import com.teleflow.orchestrator_server.dto.downstream.InventoryReserveResponse;
import com.teleflow.orchestrator_server.kafka.OrderLifecycleEvent;
import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import com.teleflow.orchestrator_server.service.DashboardBroadcastService;
import com.teleflow.orchestrator_server.service.OrderStatusService;
import com.teleflow.orchestrator_server.service.SagaEventPublisher;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryActivityImpl implements InventoryActivity {

    private final OrderStatusService orderStatusService;
    private final SagaEventPublisher sagaEventPublisher;
    private final DashboardBroadcastService dashboardBroadcastService;

    @Qualifier("inventoryWebClient")
    private final WebClient inventoryWebClient;

    @Override
    public InventoryReserveResponse reserve(String trackingId, ActivationRequestDTO request) {
        log.info("[INVENTORY] reserve start: trackingId={}", trackingId);

        // ① Update DB: step RUNNING
        orderStatusService.markStepRunning(trackingId, StepName.INVENTORY);

        // ② Kafka event
        publishEvent(trackingId, StepName.INVENTORY, StepStatus.RUNNING, OrderStatus.IN_PROGRESS, null, 0);

        // ③ WebSocket push
        broadcastUpdate(trackingId, request, StepName.INVENTORY, StepStatus.RUNNING,
                OrderStatus.IN_PROGRESS, 0, "Reserving inventory resources...");

        try {
            // ④ HTTP call to inventory-service
            InventoryReserveResponse response = inventoryWebClient.post()
                    .uri("/api/inventory/reserve")
                    .bodyValue(InventoryReserveRequest.builder()
                            .trackingId(trackingId)
                            .customerId(request.getCustomerId())
                            .planName(request.getPlanName())
                            .build())
                    .retrieve()
                    .bodyToMono(InventoryReserveResponse.class)
                    .block();  // block() is OK here — Temporal activities are synchronous

            // ⑤ Mark SUCCESS
            orderStatusService.markStepSuccess(trackingId, StepName.INVENTORY);
            publishEvent(trackingId, StepName.INVENTORY, StepStatus.SUCCESS, OrderStatus.IN_PROGRESS, null, 0);
            broadcastUpdate(trackingId, request, StepName.INVENTORY, StepStatus.SUCCESS,
                    OrderStatus.IN_PROGRESS, 0,
                    "✓ Inventory reserved: port=" + (response != null ? response.getPortId() : "N/A"));

            log.info("[INVENTORY] reserve SUCCESS: trackingId={} portId={}", trackingId,
                    response != null ? response.getPortId() : null);
            return response;

        } catch (Exception ex) {
            // Get current retry attempt from Temporal's activity context
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

            log.warn("[INVENTORY] reserve FAILED (attempt {}): trackingId={} reason={}", attempt, trackingId, reason);
            orderStatusService.markStepRetrying(trackingId, StepName.INVENTORY, reason, attempt);
            publishEvent(trackingId, StepName.INVENTORY, StepStatus.RETRYING, OrderStatus.IN_PROGRESS, reason, attempt);
            broadcastUpdate(trackingId, request, StepName.INVENTORY, StepStatus.RETRYING,
                    OrderStatus.IN_PROGRESS, attempt, "⚠ Inventory failed. Retry " + attempt + "/3...");

            throw ex; // rethrow so Temporal handles retry/compensation
        }
    }

    @Override
    public void release(String trackingId) {
        log.info("[INVENTORY] release (compensation) start: trackingId={}", trackingId);

        try {
            inventoryWebClient.post()
                    .uri("/api/inventory/release")
                    .bodyValue(java.util.Map.of("trackingId", trackingId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            orderStatusService.markStepRolledBack(trackingId, StepName.INVENTORY);
            publishEvent(trackingId, StepName.INVENTORY, StepStatus.ROLLED_BACK, OrderStatus.COMPENSATING, null, 0);
            broadcastRollback(trackingId, StepName.INVENTORY, "↩ Inventory released (rollback)");

            log.info("[INVENTORY] release SUCCESS: trackingId={}", trackingId);
        } catch (Exception ex) {
            // Compensation should be best-effort; log but don't fail the compensation saga
            log.error("[INVENTORY] release FAILED (compensation): trackingId={} error={}", trackingId, ex.getMessage());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void publishEvent(String trackingId, StepName step, StepStatus stepStatus,
                               OrderStatus orderStatus, String reason, int retry) {
        sagaEventPublisher.publish(OrderLifecycleEvent.builder()
                .trackingId(trackingId)
                .stepName(step)
                .stepStatus(stepStatus)
                .orderStatus(orderStatus)
                .reason(reason)
                .retryCount(retry)
                .build());
    }

    private void broadcastUpdate(String trackingId, ActivationRequestDTO req, StepName step,
                                  StepStatus stepStatus, OrderStatus orderStatus, int retry, String msg) {
        dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                .trackingId(trackingId)
                .customerId(req.getCustomerId())
                .planName(req.getPlanName())
                .currentStep(step)
                .stepStatus(stepStatus)
                .orderStatus(orderStatus)
                .retryCount(retry)
                .message(msg)
                .build());
    }

    private void broadcastRollback(String trackingId, StepName step, String msg) {
        dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                .trackingId(trackingId)
                .currentStep(step)
                .stepStatus(StepStatus.ROLLED_BACK)
                .orderStatus(OrderStatus.COMPENSATING)
                .message(msg)
                .build());
    }
}
