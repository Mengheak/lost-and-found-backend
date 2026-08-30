package com.group5.lostandfoundjava.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.group5.lostandfoundjava.entity.Notification;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

/**
 * One entry of the notification feed.
 *
 * <p>{@code @JsonProperty} pins the JSON name to {@code isRead}. Without it Jackson could shorten a
 * boolean called {@code isRead} to {@code read}, which would silently break existing clients.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        @JsonProperty("isRead") boolean isRead,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
