package com.teleflow.orchestrator_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP Configuration.
 *
 * ── What is STOMP? ───────────────────────────────────────────────────────
 * STOMP (Simple Text Oriented Messaging Protocol) is a messaging protocol
 * that works over WebSocket. It gives you pub/sub semantics (topics).
 *
 * Instead of raw WebSocket frames, STOMP lets you:
 *   - SUBSCRIBE to a topic: /topic/order-events
 *   - SEND to a destination
 *
 * The React/Angular frontend uses the 'sockjs-client' + '@stomp/stompjs'
 * libraries to connect and subscribe.
 *
 * ── Flow ─────────────────────────────────────────────────────────────────
 * 1. Frontend connects to: ws://localhost:8081/ws-teleflow (SockJS fallback)
 * 2. Frontend subscribes to: /topic/order-events
 * 3. Server calls DashboardBroadcastService.broadcast(dto)
 * 4. SimpMessagingTemplate.convertAndSend("/topic/order-events", dto)
 * 5. All subscribed clients receive the JSON payload instantly
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${teleflow.websocket.allowed-origins}")
    private String allowedOriginsRaw;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for /topic/** destinations
        // In production, replace with a full STOMP broker (RabbitMQ, ActiveMQ)
        registry.enableSimpleBroker("/topic");

        // Prefix for messages sent FROM client TO server (not used heavily here)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");

        registry.addEndpoint("/ws-teleflow")
                // SockJS provides WebSocket fallback for browsers that don't support WS
                .setAllowedOrigins(origins)
                .withSockJS();
    }
}
