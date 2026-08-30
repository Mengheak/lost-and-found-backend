package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.chat.ConversationResponse;
import com.group5.lostandfoundjava.dto.chat.StartConversationRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Chat threads between two users about an item. */
public interface ConversationService {

    /** Returns the existing thread when there is one, so tapping "chat" twice is harmless. */
    ConversationResponse startOrGet(UUID currentUserId, StartConversationRequest request);

    PageResponse<ConversationResponse> listForUser(UUID userId, Pageable pageable);

    ConversationResponse getForUser(UUID conversationId, UUID userId);
}
