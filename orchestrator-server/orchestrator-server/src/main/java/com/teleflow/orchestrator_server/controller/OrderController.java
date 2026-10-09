package com.teleflow.orchestrator_server.controller;

import com.teleflow.orchestrator_server.dto.ActivationRequestDTO;
import com.teleflow.orchestrator_server.dto.MetricsResponseDTO;
import com.teleflow.orchestrator_server.dto.OrderResponseDTO;
import com.teleflow.orchestrator_server.service.OrderOrchestrationService;
import com.teleflow.orchestrator_server.service.OrderStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for TelecomOrder operations.
 *
 * ── Endpoints ─────────────────────────────────────────────────────────────
 *
 *  POST   /api/orders/activate        → Start new activation saga
 *  GET    /api/orders                 → Paginated list (newest first)
 *  GET    /api/orders/{id}            → Single order with full step audit trail
 *  GET    /api/orders/metrics         → Dashboard KPIs
 *
 * ── How the controller stays thin ────────────────────────────────────────
 * All business logic lives in OrderOrchestrationService and OrderStatusService.
 * The controller only: validates input, delegates, formats HTTP responses.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderOrchestrationService orchestrationService;
    private final OrderStatusService orderStatusService;

    /**
     * POST /api/orders/activate
     *
     * Starts a new telecom service activation.
     * Returns 201 Created with the order DTO (status=IN_PROGRESS).
     * The saga runs asynchronously — use WebSocket or GET /api/orders/{id}
     * to track progress.
     *
     * Request body example:
     * {
     *   "customerId": "CUST-001",
     *   "planName": "FIBER_300MBPS",
     *   "customerEmail": "user@example.com"
     * }
     */
    @PostMapping("/activate")
    public ResponseEntity<OrderResponseDTO> activateService(
            @Valid @RequestBody ActivationRequestDTO request) {

        log.info("POST /api/orders/activate: customerId={} plan={}",
                request.getCustomerId(), request.getPlanName());

        OrderResponseDTO response = orchestrationService.startActivation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/orders
     *
     * Returns a paginated list of all orders, newest first.
     *
     * Query params:
     *   page (default 0) — page number
     *   size (default 20) — page size (max 100)
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(orderStatusService.getAllOrders(pageable));
    }

    /**
     * GET /api/orders/{id}
     *
     * Returns a single order by database ID, including the full step audit trail.
     * Throws 404 if not found (handled by GlobalExceptionHandler).
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable Long id) {
        log.debug("GET /api/orders/{}", id);
        return ResponseEntity.ok(orderStatusService.getOrderById(id));
    }

    /**
     * GET /api/orders/metrics
     *
     * Returns aggregated dashboard KPIs:
     *   - totalOrders
     *   - successCount / failedCount / inProgressCount
     *   - successRatePercent
     *   - avgActivationDurationMs
     *
     * Note: path /metrics is declared BEFORE /{id} to avoid conflict.
     */
    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponseDTO> getMetrics() {
        return ResponseEntity.ok(orderStatusService.getMetrics());
    }
}
