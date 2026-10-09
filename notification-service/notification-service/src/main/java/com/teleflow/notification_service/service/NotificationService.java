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
        String subject = "TeleFlow: Service Activated Successfully! [" + request.getTrackingId() + "]";
        String body = String.format("Dear Customer,\n\nYour telecom service plan '%s' has been successfully provisioned and activated!\nTracking ID: %s\nThank you for choosing TeleFlow.",
                request.getPlanName(), request.getTrackingId());

        log.info("[NOTIFICATION-SMS/EMAIL] Sent SUCCESS notification to {} for trackingId={}",
                recipient, request.getTrackingId());

        NotificationLog logEntry = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .recipientEmail(recipient)
                .notificationType("SUCCESS")
                .subject(subject)
                .messageBody(body)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();

        repository.save(logEntry);
    }

    @Transactional
    public void sendFailure(NotificationFailureRequest request) {
        String recipient = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail() : "customer@teleflow.internal";
        String subject = "TeleFlow: Service Activation Notice [" + request.getTrackingId() + "]";
        String body = String.format("Dear Customer,\n\nWe encountered an issue during activation step '%s'. All allocated resources and charges have been automatically rolled back.\nReason: %s\nTracking ID: %s",
                request.getFailedStep(), request.getReason(), request.getTrackingId());

        log.warn("[NOTIFICATION-SMS/EMAIL] Sent FAILURE notification to {} for trackingId={} step={}",
                recipient, request.getTrackingId(), request.getFailedStep());

        NotificationLog logEntry = NotificationLog.builder()
                .trackingId(request.getTrackingId())
                .recipientEmail(recipient)
                .notificationType("FAILURE")
                .subject(subject)
                .messageBody(body)
                .deliveryStatus("DELIVERED")
                .sentAt(LocalDateTime.now())
                .build();

        repository.save(logEntry);
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
