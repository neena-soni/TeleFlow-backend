package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import com.teleflow.orchestrator_server.kafka.OrderLifecycleEvent;
import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import com.teleflow.orchestrator_server.service.DashboardBroadcastService;
import com.teleflow.orchestrator_server.service.OrderStatusService;
import com.teleflow.orchestrator_server.service.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Implementation of NotificationActivity.
 *
 * ── Best-effort activity ──────────────────────────────────────────────────
 * Notification does NOT participate in compensation (saga rollback).
 * If a notification fails, we log the error but do not roll back the saga.
 * A sent notification cannot be "unsent", so no compensation makes sense.
 *
 * Temporal retry policy for this activity is set to maxAttempts=2 in
 * TemporalConfig (lower than other steps, since notification is non-critical).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationActivityImpl implements NotificationActivity {

    private final OrderStatusService orderStatusService;
    private final SagaEventPublisher sagaEventPublisher;
    private final DashboardBroadcastService dashboardBroadcastService;

    @Qualifier("notificationWebClient")
    private final WebClient notificationWebClient;

    @Override
    public void sendSuccess(String trackingId, String customerEmail, String planName) {
        log.info("[NOTIFICATION] sendSuccess: trackingId={} email={}", trackingId, customerEmail);

        orderStatusService.markStepRunning(trackingId, StepName.NOTIFICATION);

        try {
            String customerId = "CUST-DEFAULT";
            try {
                customerId = orderStatusService.getOrderByTrackingId(trackingId).getCustomerId();
            } catch (Exception ignored) {}

            notificationWebClient.post()
                    .uri("/api/notify/success")
                    .bodyValue(Map.of(
                            "trackingId", trackingId,
                            "customerId", customerId,
                            "email", customerEmail != null ? customerEmail : "",
                            "planName", planName
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            orderStatusService.markStepSuccess(trackingId, StepName.NOTIFICATION);
            sagaEventPublisher.publish(OrderLifecycleEvent.builder()
                    .trackingId(trackingId).stepName(StepName.NOTIFICATION)
                    .stepStatus(StepStatus.SUCCESS).orderStatus(OrderStatus.COMPLETED).build());

            dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                    .trackingId(trackingId).planName(planName)
                    .currentStep(StepName.NOTIFICATION).stepStatus(StepStatus.SUCCESS)
                    .orderStatus(OrderStatus.COMPLETED)
                    .message("✅ Service activated! Confirmation sent to " + customerEmail)
                    .build());

            log.info("[NOTIFICATION] sendSuccess DONE: trackingId={}", trackingId);

        } catch (Exception ex) {
            log.error("[NOTIFICATION] sendSuccess failed (best-effort): trackingId={} error={}",
                    trackingId, ex.getMessage());
            // Don't rethrow — notification failure doesn't roll back the saga
            orderStatusService.markStepFailed(trackingId, StepName.NOTIFICATION, ex.getMessage());
        }
    }

    @Override
    public void sendFailure(String trackingId, String customerEmail, String failedStep, String reason) {
        log.info("[NOTIFICATION] sendFailure: trackingId={} failedStep={}", trackingId, failedStep);

        try {
            String customerId = "CUST-DEFAULT";
            try {
                customerId = orderStatusService.getOrderByTrackingId(trackingId).getCustomerId();
            } catch (Exception ignored) {}

            notificationWebClient.post()
                    .uri("/api/notify/failure")
                    .bodyValue(Map.of(
                            "trackingId", trackingId,
                            "customerId", customerId,
                            "email", customerEmail != null ? customerEmail : "",
                            "failedStep", failedStep,
                            "reason", reason != null ? reason : "Unknown error"
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            // Mark step and overall order as FAILED in NeonDB database
            orderStatusService.markStepFailed(trackingId, StepName.NOTIFICATION, reason);
            orderStatusService.markOrderFailed(trackingId, "Failed at " + failedStep + ": " + reason);

            sagaEventPublisher.publish(OrderLifecycleEvent.builder()
                    .trackingId(trackingId).stepName(StepName.NOTIFICATION)
                    .stepStatus(StepStatus.FAILED).orderStatus(OrderStatus.FAILED)
                    .reason(reason).build());

            dashboardBroadcastService.broadcast(OrderStatusUpdateDTO.builder()
                    .trackingId(trackingId)
                    .currentStep(StepName.NOTIFICATION).stepStatus(StepStatus.FAILED)
                    .orderStatus(OrderStatus.FAILED)
                    .message("❌ Activation failed at " + failedStep + ". Customer notified.")
                    .build());

            log.info("[NOTIFICATION] sendFailure DONE: trackingId={}", trackingId);

        } catch (Exception ex) {
            log.error("[NOTIFICATION] sendFailure failed (best-effort): trackingId={} error={}",
                    trackingId, ex.getMessage());
            orderStatusService.markOrderFailed(trackingId, "Failed at " + failedStep + ": " + reason);
        }
    }
}
