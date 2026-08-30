package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.exception.ForbiddenException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.notification.NotificationResponse;
import com.group5.lostandfoundjava.entity.Notification;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import com.group5.lostandfoundjava.repository.NotificationRepository;
import com.group5.lostandfoundjava.service.NotificationService;
import com.group5.lostandfoundjava.service.PushSender;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushSender pushSender;

    public NotificationServiceImpl(NotificationRepository notificationRepository, PushSender pushSender) {
        this.notificationRepository = notificationRepository;
        this.pushSender = pushSender;
    }

    /** Stores the notification for the in-app feed, then mirrors it to the device as a push. */
    @Override
    @Transactional
    public void notify(User user, NotificationType type, String message) {
        notificationRepository.save(new Notification(user, type, message));
        pushSender.sendToUser(user.getId(), title(type), message);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByUserId(userId, pageable).map(NotificationResponse::from));
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("This notification does not belong to you");
        }

        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }

    /** The short headline a push notification shows above the message. */
    private String title(NotificationType type) {
        return switch (type) {
            case ITEM_SAVED -> "Your item was saved";
            case NEW_MESSAGE -> "New message";
            case NEW_RATING -> "New rating";
            case GENERAL -> "Lost & Found";
        };
    }
}
