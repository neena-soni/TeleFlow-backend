package com.teleflow.orchestrator_server.repository;

import com.teleflow.orchestrator_server.model.TelecomOrder;
import com.teleflow.orchestrator_server.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for TelecomOrder.
 *
 * Spring auto-generates all SQL — no @Query needed for basic ops.
 * Custom queries use JPQL (Java Persistence Query Language).
 */
@Repository
public interface TelecomOrderRepository extends JpaRepository<TelecomOrder, Long> {

    /**
     * Find order by UUID trackingId (used by Temporal workflow and Kafka events).
     */
    Optional<TelecomOrder> findByTrackingId(String trackingId);

    /**
     * Paginated list sorted newest-first (for dashboard list view).
     */
    Page<TelecomOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Metrics: count orders that completed successfully.
     */
    long countByStatus(OrderStatus status);

    /**
     * Metrics: calculate average activation duration for COMPLETED orders.
     */
    @Query("SELECT AVG(o.activationDurationMs) FROM TelecomOrder o WHERE o.activationDurationMs IS NOT NULL")
    Double findAverageActivationDurationMs();
}
