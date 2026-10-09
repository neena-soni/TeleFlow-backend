package com.teleflow.notification_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_tracking_id", columnList = "trackingId"),
    @Index(name = "idx_notif_type", columnList = "notificationType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String trackingId;

    @Column(length = 120)
    private String recipientEmail;

    @Column(nullable = false, length = 32)
    private String notificationType; // SUCCESS, FAILURE

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    @Column(nullable = false, length = 32)
    private String deliveryStatus; // DELIVERED, SIMULATED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;
}
