package com.teleflow.orchestrator_server.config;

import com.teleflow.orchestrator_server.temporal.activity.*;
import com.teleflow.orchestrator_server.temporal.workflow.ServiceActivationWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal.io Configuration.
 *
 * ── How Temporal works (conceptually) ───────────────────────────────────
 *
 *   [Your App]              [Temporal Server]           [Worker (this app)]
 *       │                        │                            │
 *       │── startWorkflow ──────▶│                            │
 *       │                        │── schedule activity ──────▶│
 *       │                        │                            │── execute activity
 *       │                        │◀─ activity complete ───────│
 *       │                        │── schedule next activity ─▶│
 *       │                        │                            │
 *
 * The Temporal Server is the persistence and scheduling engine.
 * The Worker (this app) hosts the actual business logic.
 * WorkflowClient sends commands TO the server; Worker polls FROM the server.
 *
 * ── Three Temporal components configured here ────────────────────────────
 *
 * 1. WorkflowServiceStubs — gRPC connection to Temporal server (localhost:7233)
 * 2. WorkflowClient — sends workflow start/signal commands to the server
 * 3. WorkerFactory + Worker — polls the "teleflow-task-queue" and executes
 *    workflow + activity code when Temporal schedules them
 *
 * ── Spring Boot Temporal Starter Alternative ─────────────────────────────
 * With @WorkflowImpl and @ActivityImpl annotations on the implementations,
 * the Spring Boot Temporal starter (io.temporal:temporal-spring-boot-starter)
 * can auto-configure everything. We use manual config here for maximum
 * transparency and learning visibility.
 */
@Slf4j
@Configuration
public class TemporalConfig {

    @Value("${temporal.service.target}")
    private String temporalTarget;     // e.g. "127.0.0.1:7233"

    @Value("${temporal.namespace}")
    private String namespace;          // e.g. "default"

    @Value("${temporal.task-queue}")
    private String taskQueue;          // "teleflow-task-queue"

    // ─── Injected activity implementations (Spring beans) ─────────────────
    // We inject the concrete implementations so Temporal's worker can
    // execute them with Spring's full DI (WebClient, services, etc.)

    private final InventoryActivityImpl inventoryActivity;
    private final NetworkActivityImpl   networkActivity;
    private final BillingActivityImpl   billingActivity;
    private final NotificationActivityImpl notificationActivity;

    public TemporalConfig(InventoryActivityImpl inventoryActivity,
                          NetworkActivityImpl networkActivity,
                          BillingActivityImpl billingActivity,
                          NotificationActivityImpl notificationActivity) {
        this.inventoryActivity   = inventoryActivity;
        this.networkActivity     = networkActivity;
        this.billingActivity     = billingActivity;
        this.notificationActivity = notificationActivity;
    }

    /**
     * gRPC channel stubs to the Temporal frontend service.
     * This is the low-level connection — WorkflowClient wraps this.
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Connecting to Temporal server at: {}", temporalTarget);
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalTarget)
                        .build());
    }

    /**
     * High-level Temporal client used to start/signal workflows.
     * Injected into OrderOrchestrationService.
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(namespace)
                        .build());
    }

    /**
     * Worker factory that creates workers for a given WorkflowClient.
     */
    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    /**
     * The actual Worker that polls "teleflow-task-queue".
     *
     * We register:
     *   - ServiceActivationWorkflowImpl (the saga brain)
     *   - All 4 ActivityImpl beans (inject Spring beans with DI)
     *
     * Worker.start() is called by the workerFactory.start() below.
     */
    @Bean
    public Worker teleflowWorker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker(taskQueue);

        // Register workflow implementation class (NOT an instance)
        worker.registerWorkflowImplementationTypes(ServiceActivationWorkflowImpl.class);

        // Register activity implementations (Spring beans — have full DI)
        worker.registerActivitiesImplementations(
                inventoryActivity,
                networkActivity,
                billingActivity,
                notificationActivity
        );

        log.info("Temporal worker registered on task-queue: {}", taskQueue);

        // Start all workers in the factory
        workerFactory.start();
        log.info("Temporal worker started, polling for tasks...");

        return worker;
    }
}
