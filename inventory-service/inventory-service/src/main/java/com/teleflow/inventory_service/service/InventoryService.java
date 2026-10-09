package com.teleflow.inventory_service.service;

import com.teleflow.inventory_service.dto.InventoryReleaseRequest;
import com.teleflow.inventory_service.dto.InventoryReserveRequest;
import com.teleflow.inventory_service.dto.InventoryReserveResponse;
import com.teleflow.inventory_service.dto.InventoryStatsDTO;
import com.teleflow.inventory_service.exception.InventoryOutOfStockException;
import com.teleflow.inventory_service.model.InventoryReservation;
import com.teleflow.inventory_service.model.ReservationStatus;
import com.teleflow.inventory_service.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryReservationRepository repository;
    private static final int TOTAL_POOL_CAPACITY = 1000;

    /**
     * Reserve / allocate physical network port and router slot for an order.
     * Idempotent: If trackingId already has an active reservation, return existing one.
     */
    @Transactional
    public InventoryReserveResponse reserve(InventoryReserveRequest request) {
        log.info("[INVENTORY] Processing reserve request: trackingId={}, customerId={}, plan={}",
                request.getTrackingId(), request.getCustomerId(), request.getPlanName());

        // ① Chaos Simulation / Failure Injection Hook for Hackathon Demos
        if (request.getPlanName() != null && request.getPlanName().toUpperCase().contains("CHAOS_FAIL_INV")
                || (request.getCustomerId() != null && request.getCustomerId().toUpperCase().startsWith("CHAOS_INV"))
                || (request.getPlanName() != null && request.getPlanName().equalsIgnoreCase("OUT_OF_STOCK"))) {
            log.error("[INVENTORY-CHAOS] Simulated out-of-stock trigger for trackingId={}", request.getTrackingId());
            throw new InventoryOutOfStockException("Simulated Failure: No network ports available in zone for plan: " + request.getPlanName());
        }

        // ② Idempotency Check: Already reserved?
        Optional<InventoryReservation> existing = repository.findByTrackingId(request.getTrackingId());
        if (existing.isPresent()) {
            InventoryReservation res = existing.get();
            log.info("[INVENTORY] Existing reservation found for trackingId={}, status={}",
                    request.getTrackingId(), res.getStatus());
            return InventoryReserveResponse.builder()
                    .trackingId(res.getTrackingId())
                    .portId(res.getPortId())
                    .routerId(res.getRouterId())
                    .status(res.getStatus().name())
                    .build();
        }

        // ③ Allocate Port & Router based on Plan type
        String planUpper = request.getPlanName() != null ? request.getPlanName().toUpperCase() : "";
        String portType;
        String portPrefix;
        String routerPrefix;

        if (planUpper.contains("5G")) {
            portType = "5G_NR_CELL";
            portPrefix = "PORT-5G-";
            routerPrefix = "RTR-5G-CORE-";
        } else if (planUpper.contains("FIBER") || planUpper.contains("BROADBAND")) {
            portType = "FIBER_GPON";
            portPrefix = "PORT-FBR-";
            routerPrefix = "OLT-CISCO-";
        } else {
            portType = "LTE_CELL";
            portPrefix = "PORT-BLR-";
            routerPrefix = "RTR-CISCO-";
        }

        int portSuffix = Math.abs((request.getTrackingId() + request.getCustomerId()).hashCode() % 900) + 100;
        int routerSuffix = Math.abs(request.getCustomerId().hashCode() % 10) + 1;

        String allocatedPort = portPrefix + portSuffix;
        String allocatedRouter = routerPrefix + String.format("%02d", routerSuffix);

        // ④ Persist new reservation
        InventoryReservation reservation = InventoryReservation.builder()
                .trackingId(request.getTrackingId())
                .customerId(request.getCustomerId())
                .planName(request.getPlanName())
                .portId(allocatedPort)
                .routerId(allocatedRouter)
                .portType(portType)
                .status(ReservationStatus.ALLOCATED)
                .reservedAt(LocalDateTime.now())
                .build();

        reservation = repository.save(reservation);
        log.info("[INVENTORY] Successfully reserved portId={} routerId={} for trackingId={}",
                allocatedPort, allocatedRouter, request.getTrackingId());

        return InventoryReserveResponse.builder()
                .trackingId(reservation.getTrackingId())
                .portId(reservation.getPortId())
                .routerId(reservation.getRouterId())
                .status(reservation.getStatus().name())
                .build();
    }

    /**
     * Release / rollback port allocation during Saga compensation.
     */
    @Transactional
    public void release(InventoryReleaseRequest request) {
        log.info("[INVENTORY-COMPENSATION] Releasing inventory for trackingId={}", request.getTrackingId());

        Optional<InventoryReservation> opt = repository.findByTrackingId(request.getTrackingId());
        if (opt.isPresent()) {
            InventoryReservation res = opt.get();
            res.setStatus(ReservationStatus.RELEASED);
            res.setReleasedAt(LocalDateTime.now());
            repository.save(res);
            log.info("[INVENTORY-COMPENSATION] Port {} router {} released successfully for trackingId={}",
                    res.getPortId(), res.getRouterId(), request.getTrackingId());
        } else {
            log.warn("[INVENTORY-COMPENSATION] No reservation found to release for trackingId={}",
                    request.getTrackingId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<InventoryReservation> getReservation(String trackingId) {
        return repository.findByTrackingId(trackingId);
    }

    @Transactional(readOnly = true)
    public List<InventoryReservation> getAllReservations() {
        return repository.findTop50ByOrderByReservedAtDesc();
    }

    @Transactional(readOnly = true)
    public InventoryStatsDTO getStats() {
        long total = repository.count();
        long active = repository.countByStatus(ReservationStatus.ALLOCATED)
                    + repository.countByStatus(ReservationStatus.RESERVED);
        long released = repository.countByStatus(ReservationStatus.RELEASED);
        int available = Math.max(0, TOTAL_POOL_CAPACITY - (int) active);

        return InventoryStatsDTO.builder()
                .totalReservations(total)
                .activeAllocations(active)
                .releasedAllocations(released)
                .poolCapacity(TOTAL_POOL_CAPACITY)
                .availableCapacity(available)
                .status("HEALTHY")
                .build();
    }
}
