package com.teleflow.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {
    private long totalNotificationsSent;
    private long successNotifications;
    private long failureNotifications;
    private String status;
}
