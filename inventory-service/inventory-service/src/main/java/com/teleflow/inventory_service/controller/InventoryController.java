package com.teleflow.inventory_service.controller;

import com.teleflow.inventory_service.dto.InventoryReleaseRequest;
import com.teleflow.inventory_service.dto.InventoryReserveRequest;
import com.teleflow.inventory_service.dto.InventoryReserveResponse;
import com.teleflow.inventory_service.dto.InventoryStatsDTO;
import com.teleflow.inventory_service.model.InventoryReservation;
import com.teleflow.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Reserve port & router resources.
     * Invoked synchronously by Orchestrator Server's Temporal InventoryActivity.
     */
    @PostMapping("/reserve")
    public ResponseEntity<InventoryReserveResponse> reserve(@Valid @RequestBody InventoryReserveRequest request) {
        log.info("[HTTP POST /api/inventory/reserve] Request: {}", request);
        InventoryReserveResponse response = inventoryService.reserve(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Release allocated resources (Saga compensation).
     * Invoked by Orchestrator Server's Temporal InventoryActivity on workflow failure/compensation.
     */
    @PostMapping("/release")
    public ResponseEntity<Map<String, String>> release(@Valid @RequestBody InventoryReleaseRequest request) {
        log.info("[HTTP POST /api/inventory/release] Request: {}", request);
        inventoryService.release(request);
        return ResponseEntity.ok(Map.of("trackingId", request.getTrackingId(), "status", "RELEASED"));
    }

    /**
     * Get reservation by trackingId.
     */
    @GetMapping("/reservations/{trackingId}")
    public ResponseEntity<InventoryReservation> getByTrackingId(@PathVariable String trackingId) {
        return inventoryService.getReservation(trackingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List recent inventory allocations (for dashboard and testing inspection).
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<InventoryReservation>> getAllReservations() {
        return ResponseEntity.ok(inventoryService.getAllReservations());
    }

    /**
     * Inventory stats & capacity metrics.
     */
    @GetMapping("/stats")
    public ResponseEntity<InventoryStatsDTO> getStats() {
        return ResponseEntity.ok(inventoryService.getStats());
    }

    /**
     * Service health ping.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "inventory-service", "status", "UP"));
    }
}
