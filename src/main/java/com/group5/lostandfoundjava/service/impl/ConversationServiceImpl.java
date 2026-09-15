package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.chat.ConversationResponse;
import com.group5.lostandfoundjava.dto.chat.StartConversationRequest;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.ForbiddenException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.mapper.ConversationMapper;
import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.ConversationService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chat threads between two users about an item. */
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;

    @Override
    @Transactional
    public ConversationResponse startOrGet(UUID currentUserId, StartConversationRequest request) {
        Item item = itemRepository
                .findById(request.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        // Talking about an item almost always means talking to whoever reported it.
        UUID otherUserId =
                request.getOtherUserId() == null ? item.getUser().getId() : request.getOtherUserId();
        if (otherUserId.equals(currentUserId)) {
            throw new BadRequestException("You cannot start a conversation with yourself");
        }

        Optional<Conversation> existing =
                conversationRepository.findByItemAndParticipants(item.getId(), currentUserId, otherUserId);
        if (existing.isPresent()) {
            return conversationMapper.toResponse(existing.get());
        }

        User currentUser =
                userRepository.findById(currentUserId).orElseThrow(() -> new NotFoundException("User not found"));
        User otherUser =
                userRepository.findById(otherUserId).orElseThrow(() -> new NotFoundException("Other user not found"));

        Conversation conversation =
                conversationRepository.save(conversationMapper.toEntity(item, currentUser, otherUser));
        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listForUser(UUID userId, Pageable pageable) {
        return PageResponse.from(
                conversationRepository.findAllForUser(userId, pageable).map(conversationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getForUser(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!conversation.isParticipant(userId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
        return conversationMapper.toResponse(conversation);
    }
}
