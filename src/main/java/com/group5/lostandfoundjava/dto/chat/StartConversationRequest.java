package com.group5.lostandfoundjava.dto.chat;

import java.util.UUID;

/**
 * Body of {@code POST /api/conversations}.
 *
 * @param otherUserId who to talk to; when left out it defaults to the item's reporter, which is the
 *     usual case
 */
public record StartConversationRequest(UUID itemId, UUID otherUserId) {}
