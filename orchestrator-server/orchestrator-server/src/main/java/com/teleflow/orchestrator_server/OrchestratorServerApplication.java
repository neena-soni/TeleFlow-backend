package com.teleflow.orchestrator_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TeleFlow Orchestrator Server — Main Application Entry Point.
 *
 * ── What starts when you run this ────────────────────────────────────────
 * 1. Spring Boot auto-configures all beans (JPA, Kafka, WebSocket, WebClient)
 * 2. Hibernate creates/updates PostgreSQL tables (ddl-auto=update)
 * 3. Kafka producer is initialized and topic is declared
 * 4. WebSocket STOMP broker starts on /ws-teleflow
 * 5. TemporalConfig connects to Temporal server (localhost:7233)
 * 6. Temporal Worker starts polling "teleflow-task-queue" for tasks
 * 7. Spring MVC listens on port 8081
 *
 * ── @EnableScheduling ────────────────────────────────────────────────────
 * Enables Spring's @Scheduled task support.
 * Used for: periodic metrics publishing, health checks, etc.
 *
 * ── How to run ───────────────────────────────────────────────────────────
 * Prerequisites (must be running BEFORE this app):
 *   1. PostgreSQL on localhost:5432  (database: teleflow_orchestrator)
 *   2. Kafka on localhost:9092
 *   3. Temporal server on localhost:7233 (run: temporal server start-dev)
 *
 * Start command:
 *   mvn spring-boot:run
 *   OR
 *   java -jar target/orchestrator-server-0.0.1-SNAPSHOT.jar
 */
@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
public class OrchestratorServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorServerApplication.class, args);
    }
}
