package com.teleflow.notification_service.repository;

import com.teleflow.notification_service.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByTrackingId(String trackingId);

    List<NotificationLog> findByCustomerIdOrderBySentAtDesc(String customerId);

    List<NotificationLog> findByTargetRoleOrderBySentAtDesc(String targetRole);

    long countByNotificationType(String notificationType);

    List<NotificationLog> findTop50ByOrderBySentAtDesc();
}
