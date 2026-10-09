package com.teleflow.orchestrator_server.service;

import com.teleflow.orchestrator_server.kafka.OrderLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing order lifecycle events to Kafka.
 *
 * ── Topic: teleflow.orders.lifecycle ──────────────────────────────────────
 * Every state change in the saga publishes a message here.
 * The message key = trackingId (UUID), ensuring all events for one order
 * go to the same Kafka partition (ordering guarantee per order).
 *
 * ── Why a wrapper service? ────────────────────────────────────────────────
 * Same reasoning as DashboardBroadcastService:
 * - Testability (mock in unit tests)
 * - Swap Kafka for another broker later without touching activities
 * - Centralized error handling for publish failures
 *
 * ── Fire-and-forget ──────────────────────────────────────────────────────
 * send() is async (returns a CompletableFuture). We don't block on it —
 * Kafka publish failures don't fail the saga step. The step result
 * (success/failure) is what matters for correctness. Kafka is the audit log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaEventPublisher {

    @Value("${teleflow.kafka.topic.lifecycle}")
    private String lifecycleTopic;

    private final KafkaTemplate<String, OrderLifecycleEvent> kafkaTemplate;

    /**
     * Publishes an OrderLifecycleEvent to Kafka asynchronously.
     *
     * @param event the lifecycle event to publish
     */
    public void publish(OrderLifecycleEvent event) {
        log.debug("Kafka publish → topic={} key={} step={} status={}",
                lifecycleTopic, event.getTrackingId(), event.getStepName(), event.getStepStatus());

        kafkaTemplate.send(lifecycleTopic, event.getTrackingId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish Kafka event for trackingId={}: {}",
                                event.getTrackingId(), ex.getMessage());
                    } else {
                        log.debug("Kafka event published: offset={}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
