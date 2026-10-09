package com.teleflow.orchestrator_server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Configuration.
 *
 * ── What is Kafka? ───────────────────────────────────────────────────────
 * Apache Kafka is a distributed event streaming platform.
 * Think of it as a persistent, ordered log of events that multiple
 * producers and consumers can read/write to independently.
 *
 * ── Why we use it here ───────────────────────────────────────────────────
 * Every saga step transition publishes an event to Kafka.
 * This creates an immutable audit trail that:
 *  1. Can be replayed (Dead Letter Queue recovery)
 *  2. Can feed analytics / metrics services
 *  3. Decouples producers from consumers
 *
 * ── Topic: teleflow.orders.lifecycle ─────────────────────────────────────
 * Each message key = trackingId (UUID)
 * Partitioning by trackingId ensures all events for one order
 * go to the same partition → guaranteed ordering per order.
 */
@Configuration
public class KafkaConfig {

    @Value("${teleflow.kafka.topic.lifecycle}")
    private String lifecycleTopic;

    /**
     * Declares the Kafka topic. Spring Kafka creates it automatically
     * if it doesn't exist (when auto.create.topics.enable=true on broker).
     *
     * partitions=3: allows 3 parallel consumers
     * replicas=1:   fine for single-broker local setup (use 3 for production)
     */
    @Bean
    public NewTopic orderLifecycleTopic() {
        return TopicBuilder.name(lifecycleTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
