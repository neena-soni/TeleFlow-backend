package com.teleflow.orchestrator_server.service;

import com.teleflow.orchestrator_server.dto.MetricsResponseDTO;
import com.teleflow.orchestrator_server.dto.OrderResponseDTO;
import com.teleflow.orchestrator_server.dto.StepExecutionDTO;
import com.teleflow.orchestrator_server.exception.OrderNotFoundException;
import com.teleflow.orchestrator_server.model.OrderStepExecution;
import com.teleflow.orchestrator_server.model.TelecomOrder;
import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import com.teleflow.orchestrator_server.repository.OrderStepExecutionRepository;
import com.teleflow.orchestrator_server.repository.TelecomOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central service for all PostgreSQL read/write operations on orders and steps.
 *
 * All public methods are @Transactional to ensure DB consistency.
 * Activities call these methods from Temporal worker threads.
 *
 * ── Important note on Temporal + Spring @Transactional ──────────────────
 * Temporal activities run on Temporal's thread pool, NOT a Spring-managed
 * thread. However, since ActivityImpl classes are Spring beans (injected
 * into TemporalConfig), Spring's AOP proxy IS active — @Transactional works.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final TelecomOrderRepository orderRepository;
    private final OrderStepExecutionRepository stepRepository;

    // ─── CREATE ─────────────────────────────────────────────────────────────

    /**
     * Persists a new order and initializes all 4 step execution records as PENDING.
     * Called by OrderOrchestrationService before starting the Temporal workflow.
     */
    @Transactional
    public TelecomOrder createOrderWithSteps(TelecomOrder order) {
        TelecomOrder saved = orderRepository.save(order);

        // Create one step record per saga step, all starting as PENDING
        List<OrderStepExecution> steps = Arrays.stream(StepName.values())
                .map(stepName -> OrderStepExecution.builder()
                        .order(saved)
                        .stepName(stepName)
                        .stepStatus(StepStatus.PENDING)
                        .retryCount(0)
                        .build())
                .collect(Collectors.toList());

        stepRepository.saveAll(steps);
        log.info("Created order id={} trackingId={} with {} steps",
                saved.getId(), saved.getTrackingId(), steps.size());
        return saved;
    }

    // ─── STEP TRANSITIONS ───────────────────────────────────────────────────

    /**
     * Marks a step as RUNNING (activity has been dispatched).
     */
    @Transactional
    public void markStepRunning(String trackingId, StepName stepName) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        OrderStepExecution step = getStep(order.getId(), stepName);
        step.setStepStatus(StepStatus.RUNNING);
        step.setStartedAt(LocalDateTime.now());
        stepRepository.save(step);
        log.debug("Step RUNNING: order={} step={}", trackingId, stepName);
    }

    /**
     * Marks a step as RETRYING and increments retry count.
     * Called when Temporal is about to retry an activity.
     */
    @Transactional
    public void markStepRetrying(String trackingId, StepName stepName, String reason, int retryCount) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        OrderStepExecution step = getStep(order.getId(), stepName);
        step.setStepStatus(StepStatus.RETRYING);
        step.setRetryCount(retryCount);
        step.setFailureReason(reason);
        stepRepository.save(step);
        log.warn("Step RETRYING: order={} step={} attempt={} reason={}", trackingId, stepName, retryCount, reason);
    }

    /**
     * Marks a step as SUCCESS.
     */
    @Transactional
    public void markStepSuccess(String trackingId, StepName stepName) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        OrderStepExecution step = getStep(order.getId(), stepName);
        step.setStepStatus(StepStatus.SUCCESS);
        step.setCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
        log.info("Step SUCCESS: order={} step={}", trackingId, stepName);
    }

    /**
     * Marks a step as FAILED (all retries exhausted).
     */
    @Transactional
    public void markStepFailed(String trackingId, StepName stepName, String reason) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        OrderStepExecution step = getStep(order.getId(), stepName);
        step.setStepStatus(StepStatus.FAILED);
        step.setFailureReason(reason);
        step.setCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
        log.error("Step FAILED: order={} step={} reason={}", trackingId, stepName, reason);
    }

    /**
     * Marks a step as ROLLED_BACK (compensation completed for this step).
     */
    @Transactional
    public void markStepRolledBack(String trackingId, StepName stepName) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        OrderStepExecution step = getStep(order.getId(), stepName);
        step.setStepStatus(StepStatus.ROLLED_BACK);
        step.setCompletedAt(LocalDateTime.now());
        stepRepository.save(step);
        log.info("Step ROLLED_BACK: order={} step={}", trackingId, stepName);
    }

    // ─── ORDER TRANSITIONS ──────────────────────────────────────────────────

    /** Transition order to IN_PROGRESS (Temporal workflow has started). */
    @Transactional
    public void markOrderInProgress(String trackingId) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        order.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(order);
    }

    /** Transition order to COMPENSATING (a step failed; rollback is running). */
    @Transactional
    public void markOrderCompensating(String trackingId) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        order.setStatus(OrderStatus.COMPENSATING);
        orderRepository.save(order);
    }

    /** Transition order to COMPLETED. Records activation duration. */
    @Transactional
    public void markOrderCompleted(String trackingId) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setActivationDurationMs(
                ChronoUnit.MILLIS.between(order.getCreatedAt(), order.getCompletedAt()));
        orderRepository.save(order);
        log.info("Order COMPLETED: trackingId={} durationMs={}", trackingId, order.getActivationDurationMs());
    }

    /** Transition order to FAILED. Records duration and failure reason. */
    @Transactional
    public void markOrderFailed(String trackingId, String failureReason) {
        TelecomOrder order = getOrderByTrackingId(trackingId);
        order.setStatus(OrderStatus.FAILED);
        order.setCompletedAt(LocalDateTime.now());
        order.setFailureReason(failureReason);
        order.setActivationDurationMs(
                ChronoUnit.MILLIS.between(order.getCreatedAt(), order.getCompletedAt()));
        orderRepository.save(order);
        log.error("Order FAILED: trackingId={} reason={}", trackingId, failureReason);
    }

    // ─── READS ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        TelecomOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public MetricsResponseDTO getMetrics() {
        long total = orderRepository.count();
        long success = orderRepository.countByStatus(OrderStatus.COMPLETED);
        long failed = orderRepository.countByStatus(OrderStatus.FAILED);
        long inProgress = orderRepository.countByStatus(OrderStatus.IN_PROGRESS)
                + orderRepository.countByStatus(OrderStatus.COMPENSATING);

        double rate = total > 0 ? Math.round((success * 100.0 / total) * 100.0) / 100.0 : 0.0;
        Double avgDuration = orderRepository.findAverageActivationDurationMs();

        return MetricsResponseDTO.builder()
                .totalOrders(total)
                .successCount(success)
                .failedCount(failed)
                .inProgressCount(inProgress)
                .successRatePercent(rate)
                .avgActivationDurationMs(avgDuration)
                .build();
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    public TelecomOrder getOrderByTrackingId(String trackingId) {
        return orderRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new OrderNotFoundException("trackingId", trackingId));
    }

    private OrderStepExecution getStep(Long orderId, StepName stepName) {
        return stepRepository.findByOrderIdAndStepName(orderId, stepName)
                .orElseThrow(() -> new OrderNotFoundException(
                        "step " + stepName + " for orderId", orderId.toString()));
    }

    private OrderResponseDTO toResponseDTO(TelecomOrder order) {
        List<StepExecutionDTO> stepDTOs = stepRepository
                .findByOrderIdOrderByStartedAtAsc(order.getId())
                .stream()
                .map(s -> StepExecutionDTO.builder()
                        .id(s.getId())
                        .stepName(s.getStepName())
                        .stepStatus(s.getStepStatus())
                        .retryCount(s.getRetryCount())
                        .failureReason(s.getFailureReason())
                        .startedAt(s.getStartedAt())
                        .completedAt(s.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .trackingId(order.getTrackingId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .planName(order.getPlanName())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .activationDurationMs(order.getActivationDurationMs())
                .failureReason(order.getFailureReason())
                .steps(stepDTOs)
                .build();
    }
}
