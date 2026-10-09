package com.teleflow.billing_service.controller;

import com.teleflow.billing_service.dto.BillingChargeRequest;
import com.teleflow.billing_service.dto.BillingChargeResponse;
import com.teleflow.billing_service.dto.BillingRefundRequest;
import com.teleflow.billing_service.dto.BillingStatsDTO;
import com.teleflow.billing_service.model.BillingCharge;
import com.teleflow.billing_service.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/charge")
    public ResponseEntity<BillingChargeResponse> charge(@Valid @RequestBody BillingChargeRequest request) {
        log.info("[HTTP POST /api/billing/charge] Request: {}", request);
        BillingChargeResponse response = billingService.charge(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, String>> refund(@Valid @RequestBody BillingRefundRequest request) {
        log.info("[HTTP POST /api/billing/refund] Request: {}", request);
        billingService.refund(request);
        return ResponseEntity.ok(Map.of("trackingId", request.getTrackingId(), "status", "REFUNDED"));
    }

    @PostMapping("/chaos/toggle")
    public ResponseEntity<Map<String, Object>> toggleChaos() {
        boolean active = billingService.toggleChaosMode();
        return ResponseEntity.ok(Map.of("chaosModeActive", active, "message", active ? "Chaos Mode ENABLED (Bank Gateway will fail)" : "Chaos Mode DISABLED (Happy path restored)"));
    }

    @GetMapping("/charges/{trackingId}")
    public ResponseEntity<BillingCharge> getByTrackingId(@PathVariable String trackingId) {
        return billingService.getCharge(trackingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/charges")
    public ResponseEntity<List<BillingCharge>> getAllCharges() {
        return ResponseEntity.ok(billingService.getAllCharges());
    }

    @GetMapping("/stats")
    public ResponseEntity<BillingStatsDTO> getStats() {
        return ResponseEntity.ok(billingService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "billing-service", "status", "UP"));
    }
}
