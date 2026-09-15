package com.group5.lostandfoundjava.dto.chat;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One chat message
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID id;

    private UUID conversationId;

    private UUID senderId;

    private String text;

    private String imageUrl;

    private Instant createdAt;
}
