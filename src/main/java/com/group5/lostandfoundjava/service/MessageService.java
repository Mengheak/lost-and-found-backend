package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.chat.MessageResponse;
import com.group5.lostandfoundjava.dto.chat.SendMessageRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Messages inside a conversation. Both the REST endpoint and the WebSocket controller call
 * {@link #send}, so a message behaves the same whichever way it arrives.
 */
public interface MessageService {

    MessageResponse send(UUID senderId, UUID conversationId, SendMessageRequest request);

    PageResponse<MessageResponse> list(UUID userId, UUID conversationId, Pageable pageable);
}
