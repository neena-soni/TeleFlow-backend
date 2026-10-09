package com.teleflow.orchestrator_server.service;

import com.teleflow.orchestrator_server.dto.OrderStatusUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for broadcasting real-time order status updates
 * to all connected WebSocket (STOMP) clients.
 *
 * ── Why a wrapper service instead of using SimpMessagingTemplate directly? ──
 * 1. Testability: You can mock DashboardBroadcastService in unit tests
 *    without dealing with WebSocket internals.
 * 2. Single responsibility: If we change to SSE or long-polling later,
 *    only this class changes.
 * 3. Logging: All broadcasts are logged here centrally.
 *
 * ── How it works ─────────────────────────────────────────────────────────
 * SimpMessagingTemplate.convertAndSend() serializes the DTO to JSON
 * and sends it to all clients subscribed to the given topic.
 *
 * Frontend subscription:
 *   stompClient.subscribe('/topic/order-events', (frame) => {
 *     const update = JSON.parse(frame.body);
 *     // update the UI with the new step status
 *   });
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardBroadcastService {

    private static final String ORDER_EVENTS_TOPIC = "/topic/order-events";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts an order status update to all subscribed WebSocket clients.
     *
     * @param update the status payload to broadcast
     */
    public void broadcast(OrderStatusUpdateDTO update) {
        log.debug("WebSocket broadcast → {} | order={} step={} status={}",
                ORDER_EVENTS_TOPIC, update.getOrderId(), update.getCurrentStep(), update.getStepStatus());

        messagingTemplate.convertAndSend(ORDER_EVENTS_TOPIC, update);
    }
}
