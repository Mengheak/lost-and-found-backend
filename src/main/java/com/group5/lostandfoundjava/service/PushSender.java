package com.group5.lostandfoundjava.service;

import java.util.UUID;

// Sends a push notification to a user's devices
public interface PushSender {

    void sendToUser(UUID userId, String title, String body);
}
