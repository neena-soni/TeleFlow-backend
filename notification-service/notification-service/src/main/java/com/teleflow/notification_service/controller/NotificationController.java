package com.teleflow.notification_service.controller;

import com.teleflow.notification_service.dto.NotificationFailureRequest;
import com.teleflow.notification_service.dto.NotificationStatsDTO;
import com.teleflow.notification_service.dto.NotificationSuccessRequest;
import com.teleflow.notification_service.model.NotificationLog;
import com.teleflow.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/success")
    public ResponseEntity<Map<String, String>> notifySuccess(@RequestBody NotificationSuccessRequest request) {
        log.info("[HTTP POST /api/notify/success] Request: {}", request);
        notificationService.sendSuccess(request);
        return ResponseEntity.ok(Map.of("trackingId", request.getTrackingId(), "status", "SENT"));
    }

    @PostMapping("/failure")
    public ResponseEntity<Map<String, String>> notifyFailure(@RequestBody NotificationFailureRequest request) {
        log.info("[HTTP POST /api/notify/failure] Request: {}", request);
        notificationService.sendFailure(request);
        return ResponseEntity.ok(Map.of("trackingId", request.getTrackingId(), "status", "SENT"));
    }

    /**
     * Customer Window / Dashboard: Fetch notifications for logged-in user
     */
    @GetMapping("/user/{customerId}")
    public ResponseEntity<List<NotificationLog>> getUserNotifications(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(customerId));
    }

    /**
     * Admin Dashboard: Fetch technical system audit alerts
     */
    @GetMapping("/admin")
    public ResponseEntity<List<NotificationLog>> getAdminAlerts() {
        return ResponseEntity.ok(notificationService.getAdminAlerts());
    }

    @GetMapping("/logs/{trackingId}")
    public ResponseEntity<List<NotificationLog>> getLogsByTrackingId(@PathVariable String trackingId) {
        return ResponseEntity.ok(notificationService.getLogsByTrackingId(trackingId));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<NotificationLog>> getAllLogs() {
        return ResponseEntity.ok(notificationService.getAllLogs());
    }

    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDTO> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "notification-service", "status", "UP"));
    }
}
