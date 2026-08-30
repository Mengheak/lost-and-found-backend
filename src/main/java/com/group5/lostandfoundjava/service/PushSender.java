package com.group5.lostandfoundjava.service;

import java.util.UUID;

/**
 * Sends a push notification to a user's devices.
 *
 * <p>Two implementations exist and {@link com.group5.lostandfoundjava.config.PushConfig} picks one
 * at startup: a real Firebase sender, or a no-op used when push is not configured.
 */
public interface PushSender {

    void sendToUser(UUID userId, String title, String body);
}
