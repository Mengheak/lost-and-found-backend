package com.group5.lostandfoundjava.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.group5.lostandfoundjava.service.PushSender;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends push notifications through Firebase Cloud Messaging.
 *
 * <p>Messages go to the topic {@code user-<id>}: the client subscribes to its own topic after
 * logging in, which means the backend never has to store device tokens.
 *
 * <p>Created by {@link com.group5.lostandfoundjava.config.PushConfig} rather than annotated as a
 * bean, because it only exists when Firebase credentials are configured.
 */
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void sendToUser(UUID userId, String title, String body) {
        try {
            Message message = Message.builder()
                    .setTopic("user-" + userId)
                    .setNotification(
                            Notification.builder().setTitle(title).setBody(body).build())
                    .build();
            firebaseMessaging.send(message);
        } catch (Exception ex) {
            // A push is a nice-to-have. Failing to deliver one must never fail the request that
            // triggered it, so the error is logged and swallowed.
            log.warn("Failed to send FCM push to user {}: {}", userId, ex.getMessage());
        }
    }
}
