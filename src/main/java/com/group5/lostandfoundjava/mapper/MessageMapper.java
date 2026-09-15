package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.chat.MessageResponse;
import com.group5.lostandfoundjava.dto.chat.SendMessageRequest;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Message;
import com.group5.lostandfoundjava.entity.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    // The sender is taken from the authenticated conversation participant
    public Message toEntity(SendMessageRequest request, Conversation conversation, User sender) {
        if (request == null) {
            return null;
        }
        return new Message(
                conversation, sender, trimmed(request.getText()), trimmed(request.getImageUrl()));
    }

    public MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .text(message.getText())
                .imageUrl(message.getImageUrl())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public List<MessageResponse> toResponseList(List<Message> messages) {
        if (messages == null) {
            return List.of();
        }
        List<MessageResponse> responses = new ArrayList<>(messages.size());
        messages.forEach(message -> responses.add(toResponse(message)));
        return responses;
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
