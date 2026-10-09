package com.teleflow.network_service.controller;

import com.teleflow.network_service.dto.NetworkActivateRequest;
import com.teleflow.network_service.dto.NetworkActivateResponse;
import com.teleflow.network_service.dto.NetworkDeactivateRequest;
import com.teleflow.network_service.dto.NetworkStatsDTO;
import com.teleflow.network_service.model.NetworkProvision;
import com.teleflow.network_service.service.NetworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkService networkService;

    @PostMapping("/activate")
    public ResponseEntity<NetworkActivateResponse> activate(@Valid @RequestBody NetworkActivateRequest request) {
        log.info("[HTTP POST /api/network/activate] Request: {}", request);
        NetworkActivateResponse response = networkService.activate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivate(@Valid @RequestBody NetworkDeactivateRequest request) {
        log.info("[HTTP POST /api/network/deactivate] Request: {}", request);
        networkService.deactivate(request);
        return ResponseEntity.ok(Map.of("trackingId", request.getTrackingId(), "status", "DEACTIVATED"));
    }

    @GetMapping("/provisions/{trackingId}")
    public ResponseEntity<NetworkProvision> getByTrackingId(@PathVariable String trackingId) {
        return networkService.getProvision(trackingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/provisions")
    public ResponseEntity<List<NetworkProvision>> getAllProvisions() {
        return ResponseEntity.ok(networkService.getAllProvisions());
    }

    @GetMapping("/stats")
    public ResponseEntity<NetworkStatsDTO> getStats() {
        return ResponseEntity.ok(networkService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "network-service", "status", "UP"));
    }
}
