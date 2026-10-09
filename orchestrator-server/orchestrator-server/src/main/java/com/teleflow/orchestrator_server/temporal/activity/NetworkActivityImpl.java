package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import com.teleflow.orchestrator_server.dto.downstream.NetworkActivateRequest;
import com.teleflow.orchestrator_server.dto.downstream.NetworkActivateResponse;
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

/**
 * Implementation of NetworkActivity.
 * Calls network-service to provision/teardown VLAN and bandwidth profiles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetworkActivityImpl implements NetworkActivity {

    private final OrderStatusService orderStatusService;
    private final SagaEventPublisher sagaEventPublisher;
    private final DashboardBroadcastService dashboardBroadcastService;

    @Qualifier("networkWebClient")
    private final WebClient networkWebClient;

    @Override
    public NetworkActivateResponse activate(String trackingId, ActivationRequestDTO request,
                                             String portId, String routerId) {
        log.info("[NETWORK] activate start: trackingId={} portId={}", trackingId, portId);

        orderStatusService.markStepRunning(trackingId, StepName.NETWORK);
        publishEvent(trackingId, StepName.NETWORK, StepStatus.RUNNING, OrderStatus.IN_PROGRESS, null, 0);
        broadcastUpdate(trackingId, request, StepName.NETWORK, StepStatus.RUNNING,
                OrderStatus.IN_PROGRESS, 0, "Provisioning network slice (VLAN + bandwidth)...");

        try {
            NetworkActivateResponse response = networkWebClient.post()
                    .uri("/api/network/activate")
                    .bodyValue(NetworkActivateRequest.builder()
                            .trackingId(trackingId)
                            .customerId(request.getCustomerId())
                            .planName(request.getPlanName())
                            .portId(portId)
                            .routerId(routerId)
                            .build())
                    .retrieve()
                    .bodyToMono(NetworkActivateResponse.class)
                    .block();

            orderStatusService.markStepSuccess(trackingId, StepName.NETWORK);
            publishEvent(trackingId, StepName.NETWORK, StepStatus.SUCCESS, OrderStatus.IN_PROGRESS, null, 0);
            broadcastUpdate(trackingId, request, StepName.NETWORK, StepStatus.SUCCESS,
                    OrderStatus.IN_PROGRESS, 0,
                    "✓ Network provisioned: " + (response != null ? response.getVlanTag() : "N/A")
                    + " @ " + (response != null ? response.getAllocatedBandwidthMbps() : "?") + " Mbps");

            log.info("[NETWORK] activate SUCCESS: trackingId={}", trackingId);
            return response;

        } catch (Exception ex) {
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.warn("[NETWORK] activate FAILED (attempt {}): trackingId={}", attempt, trackingId);
            orderStatusService.markStepRetrying(trackingId, StepName.NETWORK, reason, attempt);
            publishEvent(trackingId, StepName.NETWORK, StepStatus.RETRYING, OrderStatus.IN_PROGRESS, reason, attempt);
            broadcastUpdate(trackingId, request, StepName.NETWORK, StepStatus.RETRYING,
                    OrderStatus.IN_PROGRESS, attempt, "⚠ Network failed. Retry " + attempt + "/3...");
            throw ex;
        }
    }

    @Override
    public void deactivate(String trackingId) {
        log.info("[NETWORK] deactivate (compensation) start: trackingId={}", trackingId);
        try {
            networkWebClient.post()
                    .uri("/api/network/deactivate")
                    .bodyValue(java.util.Map.of("trackingId", trackingId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            orderStatusService.markStepRolledBack(trackingId, StepName.NETWORK);
            publishEvent(trackingId, StepName.NETWORK, StepStatus.ROLLED_BACK, OrderStatus.COMPENSATING, null, 0);
            broadcastRollback(trackingId, StepName.NETWORK, "↩ Network deactivated (rollback)");
            log.info("[NETWORK] deactivate SUCCESS: trackingId={}", trackingId);
        } catch (Exception ex) {
            log.error("[NETWORK] deactivate FAILED (compensation): trackingId={} error={}", trackingId, ex.getMessage());
        }
    }

    private void publishEvent(String trackingId, StepName step, StepStatus stepStatus,
                               OrderStatus orderStatus, String reason, int retry) {
        sagaEventPublisher.publish(OrderLifecycleEvent.builder()
                .trackingId(trackingId).stepName(step).stepStatus(stepStatus)
                .orderStatus(orderStatus).reason(reason).retryCount(retry).build());
    }

    private void broadcastUpdate(String trackingId, ActivationRequestDTO req, StepName step,
                                  StepStatus stepStatus, OrderStatus orderStatus, int retry, String msg) {
        dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                .trackingId(trackingId).customerId(req.getCustomerId()).planName(req.getPlanName())
                .currentStep(step).stepStatus(stepStatus).orderStatus(orderStatus)
                .retryCount(retry).message(msg).build());
    }

    private void broadcastRollback(String trackingId, StepName step, String msg) {
        dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                .trackingId(trackingId).currentStep(step)
                .stepStatus(StepStatus.ROLLED_BACK).orderStatus(OrderStatus.COMPENSATING)
                .message(msg).build());
    }
}
