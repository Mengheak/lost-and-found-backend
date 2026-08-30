package com.group5.lostandfoundjava.dto.chat;

import com.group5.lostandfoundjava.entity.Message;
import java.time.Instant;
import java.util.UUID;

/** One chat message. This is also exactly what WebSocket subscribers receive. */
public record MessageResponse(
        UUID id, UUID conversationId, UUID senderId, String text, String imageUrl, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getText(),
                message.getImageUrl(),
                message.getCreatedAt());
    }
}
