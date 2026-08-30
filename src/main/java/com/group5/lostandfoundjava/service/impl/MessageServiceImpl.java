package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.ForbiddenException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.chat.MessageResponse;
import com.group5.lostandfoundjava.dto.chat.SendMessageRequest;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Message;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.repository.MessageRepository;
import com.group5.lostandfoundjava.service.MessageService;
import com.group5.lostandfoundjava.service.NotificationService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Stores the message, pushes it to everyone watching the thread over WebSocket, and notifies the
     * recipient. Doing all three here is why a message sent over REST and one sent over STOMP look
     * identical to the other side.
     */
    @Override
    @Transactional
    public MessageResponse send(UUID senderId, UUID conversationId, SendMessageRequest request) {
        boolean noText = request.text() == null || request.text().isBlank();
        boolean noImage = request.imageUrl() == null || request.imageUrl().isBlank();
        if (noText && noImage) {
            throw new BadRequestException("A message must contain text or an image");
        }

        Conversation conversation = findConversationForParticipant(conversationId, senderId);
        User sender = conversation.getUserA().getId().equals(senderId)
                ? conversation.getUserA()
                : conversation.getUserB();

        Message message = messageRepository.save(new Message(
                conversation,
                sender,
                request.text() == null ? null : request.text().trim(),
                request.imageUrl() == null ? null : request.imageUrl().trim()));

        MessageResponse response = MessageResponse.from(message);

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        notificationService.notify(
                conversation.otherParticipant(senderId),
                NotificationType.NEW_MESSAGE,
                "New message from " + sender.getName());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> list(UUID userId, UUID conversationId, Pageable pageable) {
        // Called for the access check; the conversation itself is not needed here.
        findConversationForParticipant(conversationId, userId);

        return PageResponse.from(
                messageRepository.findByConversationId(conversationId, pageable).map(MessageResponse::from));
    }

    private Conversation findConversationForParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!conversation.isParticipant(userId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
        return conversation;
    }
}
