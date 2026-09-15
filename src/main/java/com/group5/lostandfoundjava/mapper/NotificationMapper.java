package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.notification.NotificationResponse;
import com.group5.lostandfoundjava.entity.Notification;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    /**
     * Notifications are raised by the application rather than posted by a client, so there is no
     * request DTO to map from — the three things a notification needs are passed directly.
     */
    public Notification toEntity(User user, NotificationType type, String message) {
        return new Notification(user, type, message);
    }

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public List<NotificationResponse> toResponseList(List<Notification> notifications) {
        if (notifications == null) {
            return List.of();
        }
        List<NotificationResponse> responses = new ArrayList<>(notifications.size());
        notifications.forEach(notification -> responses.add(toResponse(notification)));
        return responses;
    }
}
