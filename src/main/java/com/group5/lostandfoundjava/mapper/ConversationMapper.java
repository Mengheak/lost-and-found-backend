package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.chat.ConversationResponse;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public Conversation toEntity(Item item, User currentUser, User otherUser) {
        return new Conversation(item, currentUser, otherUser);
    }

    public ConversationResponse toResponse(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        return ConversationResponse.builder()
                .id(conversation.getId())
                .item(itemMapper.toSummaryResponse(conversation.getItem()))
                .userA(userMapper.toSummaryResponse(conversation.getUserA()))
                .userB(userMapper.toSummaryResponse(conversation.getUserB()))
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    public List<ConversationResponse> toResponseList(List<Conversation> conversations) {
        if (conversations == null) {
            return List.of();
        }
        List<ConversationResponse> responses = new ArrayList<>(conversations.size());
        conversations.forEach(conversation -> responses.add(toResponse(conversation)));
        return responses;
    }
}
