package com.teleflow.orchestrator_server.temporal.activity;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import com.teleflow.orchestrator_server.dto.downstream.BillingChargeRequest;
import com.teleflow.orchestrator_server.dto.downstream.BillingChargeResponse;
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

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementation of BillingActivity.
 *
 * ── This is the "chaos" step ─────────────────────────────────────────────
 * In the demo, the billing-service has a chaos switch that returns HTTP 500.
 * When that happens:
 *   1. This method throws WebClientResponseException
 *   2. Temporal catches it and retries (attempt 2, attempt 3...)
 *   3. After maxAttempts=3, Temporal throws ActivityFailure to the Workflow
 *   4. The Workflow's catch block triggers the Saga compensation
 *
 * Judges can see Retry 1/3, 2/3, 3/3 in the Temporal Web UI AND the dashboard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingActivityImpl implements BillingActivity {

    private final OrderStatusService orderStatusService;
    private final SagaEventPublisher sagaEventPublisher;
    private final DashboardBroadcastService dashboardBroadcastService;

    @Qualifier("billingWebClient")
    private final WebClient billingWebClient;

    /** Plan → monthly charge mapping (INR). */
    private static final Map<String, BigDecimal> PLAN_PRICES = Map.of(
            "FIBER_100MBPS", new BigDecimal("699.00"),
            "FIBER_300MBPS", new BigDecimal("999.00"),
            "5G_BASIC",      new BigDecimal("299.00"),
            "5G_UNLIMITED",  new BigDecimal("799.00")
    );

    @Override
    public BillingChargeResponse charge(String trackingId, ActivationRequestDTO request) {
        log.info("[BILLING] charge start: trackingId={} plan={}", trackingId, request.getPlanName());

        orderStatusService.markStepRunning(trackingId, StepName.BILLING);
        publishEvent(trackingId, StepName.BILLING, StepStatus.RUNNING, OrderStatus.IN_PROGRESS, null, 0);
        broadcastUpdate(trackingId, request, StepName.BILLING, StepStatus.RUNNING,
                OrderStatus.IN_PROGRESS, 0, "Processing payment...");

        BigDecimal amount = PLAN_PRICES.getOrDefault(request.getPlanName(), new BigDecimal("499.00"));

        try {
            BillingChargeResponse response = billingWebClient.post()
                    .uri("/api/billing/charge")
                    .bodyValue(BillingChargeRequest.builder()
                            .trackingId(trackingId)
                            .customerId(request.getCustomerId())
                            .planName(request.getPlanName())
                            .amount(amount)
                            .build())
                    .retrieve()
                    .bodyToMono(BillingChargeResponse.class)
                    .block();

            orderStatusService.markStepSuccess(trackingId, StepName.BILLING);
            publishEvent(trackingId, StepName.BILLING, StepStatus.SUCCESS, OrderStatus.IN_PROGRESS, null, 0);
            broadcastUpdate(trackingId, request, StepName.BILLING, StepStatus.SUCCESS,
                    OrderStatus.IN_PROGRESS, 0,
                    "✓ Payment processed: ₹" + amount + " charged");

            log.info("[BILLING] charge SUCCESS: trackingId={} amount={}", trackingId, amount);
            return response;

        } catch (Exception ex) {
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.warn("[BILLING] charge FAILED (attempt {}): trackingId={} reason={}", attempt, trackingId, reason);
            orderStatusService.markStepRetrying(trackingId, StepName.BILLING, reason, attempt);
            publishEvent(trackingId, StepName.BILLING, StepStatus.RETRYING, OrderStatus.IN_PROGRESS, reason, attempt);
            broadcastUpdate(trackingId, request, StepName.BILLING, StepStatus.RETRYING,
                    OrderStatus.IN_PROGRESS, attempt,
                    "⚠ Billing failed (Chaos Mode). Retry " + attempt + "/3...");
            throw ex;
        }
    }

    @Override
    public void refund(String trackingId) {
        log.info("[BILLING] refund (compensation) start: trackingId={}", trackingId);
        try {
            billingWebClient.post()
                    .uri("/api/billing/refund")
                    .bodyValue(Map.of("trackingId", trackingId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            orderStatusService.markStepRolledBack(trackingId, StepName.BILLING);
            publishEvent(trackingId, StepName.BILLING, StepStatus.ROLLED_BACK, OrderStatus.COMPENSATING, null, 0);
            broadcastRollback(trackingId, StepName.BILLING, "↩ Billing refunded (rollback)");
            log.info("[BILLING] refund SUCCESS: trackingId={}", trackingId);
        } catch (Exception ex) {
            log.error("[BILLING] refund FAILED (compensation): trackingId={} error={}", trackingId, ex.getMessage());
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
