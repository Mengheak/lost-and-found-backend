package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.service.PushSender;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in used when Firebase is not configured — it just logs and returns.
 *
 * <p>Supplying a do-nothing implementation instead of leaving the dependency null means
 * {@link com.group5.lostandfoundjava.service.impl.NotificationServiceImpl} never needs a
 * "is push enabled?" check.
 */
public class NoopPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(NoopPushSender.class);

    @Override
    public void sendToUser(UUID userId, String title, String body) {
        log.debug("Push notifications disabled — skipping push to user {}: {}", userId, title);
    }
}
