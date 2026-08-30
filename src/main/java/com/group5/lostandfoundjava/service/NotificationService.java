package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.notification.NotificationResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** The in-app notification feed. */
public interface NotificationService {

    /**
     * Records a notification for a user and, when push is configured, sends it to their device.
     * Called by the other services rather than by a controller.
     */
    void notify(User user, NotificationType type, String message);

    PageResponse<NotificationResponse> list(UUID userId, Pageable pageable);

    NotificationResponse markRead(UUID userId, UUID notificationId);

    /**
     * Marks the caller's whole feed as read.
     *
     * @return how many notifications were still unread
     */
    int markAllRead(UUID userId);
}
