package com.group5.lostandfoundjava.dto.chat;

import java.util.UUID;


public record StartConversationRequest(UUID itemId, UUID otherUserId) {}
