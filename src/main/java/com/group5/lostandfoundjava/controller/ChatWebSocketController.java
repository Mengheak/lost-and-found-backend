package com.group5.lostandfoundjava.controller;

import com.group5.lostandfoundjava.dto.chat.SendMessageRequest;
import com.group5.lostandfoundjava.service.MessageService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

// The WebSocket half of the chat
@Controller
public class ChatWebSocketController {

    private final MessageService messageService;

    public ChatWebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(
            @DestinationVariable UUID conversationId,
            @Payload SendMessageRequest request,
            Principal principal) {
        messageService.send(UUID.fromString(principal.getName()), conversationId, request);
    }
}
