package com.teleflow.inventory_service.repository;

import com.teleflow.inventory_service.model.InventoryReservation;
import com.teleflow.inventory_service.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByTrackingId(String trackingId);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(ReservationStatus status);

    List<InventoryReservation> findByCustomerId(String customerId);

    List<InventoryReservation> findTop50ByOrderByReservedAtDesc();
}
