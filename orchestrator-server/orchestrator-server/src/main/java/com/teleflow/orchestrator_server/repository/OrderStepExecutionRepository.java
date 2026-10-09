package com.teleflow.orchestrator_server.repository;

import com.teleflow.orchestrator_server.model.OrderStepExecution;
import com.teleflow.orchestrator_server.model.enums.StepName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for OrderStepExecution.
 */
@Repository
public interface OrderStepExecutionRepository extends JpaRepository<OrderStepExecution, Long> {

    /**
     * Get all steps for a given order, ordered by step start time.
     */
    List<OrderStepExecution> findByOrderIdOrderByStartedAtAsc(Long orderId);

    /**
     * Find a specific step within an order (e.g., to update BILLING step status).
     */
    Optional<OrderStepExecution> findByOrderIdAndStepName(Long orderId, StepName stepName);
}
