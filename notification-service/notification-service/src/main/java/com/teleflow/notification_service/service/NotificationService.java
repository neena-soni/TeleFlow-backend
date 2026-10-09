package com.teleflow.notification_service.service;

import com.teleflow.notification_service.dto.NotificationFailureRequest;
import com.teleflow.notification_service.dto.NotificationStatsDTO;
import com.teleflow.notification_service.dto.NotificationSuccessRequest;
import com.teleflow.notification_service.model.NotificationLog;
import com.teleflow.notification_service.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository repository;

    @Transactional
    public void sendSuccess(NotificationSuccessRequest request) {
        String recipient = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail() : "customer@teleflow.internal";
        String customerId = (request.getCustomerId() != null && !request.getCustomerId().isBlank())
                ? request.getCustomerId() : "CUST-DEFAULT";

        // ① Customer-Facing Notification (ROLE_USER)
        String userSubject = "TeleFlow: Service Activated Successfully! [" + request.getTrackingId() + "]";
        String userBody = String.format("Dear Customer,\n\nYour telecom service plan '%s' has been successfully provisioned and activated!\nTracking ID: %s\nThank you for choosing TeleFlow.",
                request.getPlanName(), request.getTrackingId());

        NotificationLog userLog = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .customerId(customerId)
                .targetRole("ROLE_USER")
                .recipientEmail(recipient)
                .notificationType("SUCCESS")
                .subject(userSubject)
                .messageBody(userBody)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();
        repository.save(userLog);

        // ② Operations / Admin-Facing Audit Alert (ROLE_ADMIN)
        String adminSubject = "[ADMIN AUDIT] Order Activation SUCCESS [" + request.getTrackingId() + "]";
        String adminBody = String.format("ORDER ACTIVATION COMPLETED:\n- Tracking ID: %s\n- Customer ID: %s\n- Plan: %s\n- All microservices (Inventory, Network, Billing) executed with 0 errors.",
                request.getTrackingId(), customerId, request.getPlanName());

        NotificationLog adminLog = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .customerId(customerId)
                .targetRole("ROLE_ADMIN")
                .recipientEmail("admin-alerts@teleflow.internal")
                .notificationType("SUCCESS")
                .subject(adminSubject)
                .messageBody(adminBody)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();
        repository.save(adminLog);

        log.info("[NOTIFICATION] Dual dispatch (USER + ADMIN) SUCCESS logged for trackingId={}", request.getTrackingId());
    }

    @Transactional
    public void sendFailure(NotificationFailureRequest request) {
        String recipient = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail() : "customer@teleflow.internal";
        String customerId = (request.getCustomerId() != null && !request.getCustomerId().isBlank())
                ? request.getCustomerId() : "CUST-DEFAULT";

        // ① Customer-Facing Reassurance Notification (ROLE_USER)
        String userSubject = "TeleFlow Notice: Order Cancelled & Charges Refunded [" + request.getTrackingId() + "]";
        String userBody = String.format("Dear Customer,\n\nWe encountered a technical issue during activation step '%s'. All allocated resources and payment charges have been automatically refunded to you.\nReason: %s\nTracking ID: %s",
                request.getFailedStep(), request.getReason(), request.getTrackingId());

        NotificationLog userLog = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .customerId(customerId)
                .targetRole("ROLE_USER")
                .recipientEmail(recipient)
                .notificationType("FAILURE")
                .subject(userSubject)
                .messageBody(userBody)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();
        repository.save(userLog);

        // ② Admin / NOC Alert: Critical Saga Rollback Audit (ROLE_ADMIN)
        String adminSubject = "[CRITICAL ADMIN ALERT] Saga Rollback Completed [" + request.getTrackingId() + "]";
        String adminBody = String.format("SAGA ROLLBACK EXECUTION REPORT:\n- Tracking ID: %s\n- Customer ID: %s\n- Failed Activity: %s\n- Root Cause: %s\n- Compensation Status: All upstream microservices successfully compensated and zero resource leak detected.",
                request.getTrackingId(), customerId, request.getFailedStep(), request.getReason());

        NotificationLog adminLog = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .customerId(customerId)
                .targetRole("ROLE_ADMIN")
                .recipientEmail("admin-alerts@teleflow.internal")
                .notificationType("FAILURE")
                .subject(adminSubject)
                .messageBody(adminBody)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();
        repository.save(adminLog);

        log.warn("[NOTIFICATION] Dual dispatch (USER + ADMIN) SAGA ROLLBACK logged for trackingId={}", request.getTrackingId());
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getUserNotifications(String customerId) {
        return repository.findByCustomerIdOrderBySentAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getAdminAlerts() {
        return repository.findByTargetRoleOrderBySentAtDesc("ROLE_ADMIN");
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getLogsByTrackingId(String trackingId) {
        return repository.findByTrackingId(trackingId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getAllLogs() {
        return repository.findTop50ByOrderBySentAtDesc();
    }

    @Transactional(readOnly = true)
    public NotificationStatsDTO getStats() {
        long total = repository.count();
        long success = repository.countByNotificationType("SUCCESS");
        long failure = repository.countByNotificationType("FAILURE");

        return NotificationStatsDTO.builder()
                .totalNotificationsSent(total)
                .successNotifications(success)
                .failureNotifications(failure)
                .status("HEALTHY")
                .build();
    }
}
