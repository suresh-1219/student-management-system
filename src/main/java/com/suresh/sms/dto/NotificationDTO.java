package com.suresh.sms.dto;

import java.time.LocalDateTime;

import com.suresh.sms.entity.NotificationType;

public record NotificationDTO(
        Long id,
        NotificationType type,
        String message,
        Long referenceId,
        LocalDateTime createdAt
) {
}
