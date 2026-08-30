package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.ForbiddenException;
import com.group5.lostandfoundjava.common.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.chat.ConversationResponse;
import com.group5.lostandfoundjava.dto.chat.StartConversationRequest;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.ConversationService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chat threads between two users about an item. */
@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ConversationServiceImpl(
            ConversationRepository conversationRepository,
            ItemRepository itemRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ConversationResponse startOrGet(UUID currentUserId, StartConversationRequest request) {
        Item item =
                itemRepository.findById(request.itemId()).orElseThrow(() -> new NotFoundException("Item not found"));

        // Talking about an item almost always means talking to whoever reported it.
        UUID otherUserId = request.otherUserId() == null ? item.getUser().getId() : request.otherUserId();
        if (otherUserId.equals(currentUserId)) {
            throw new BadRequestException("You cannot start a conversation with yourself");
        }

        Optional<Conversation> existing =
                conversationRepository.findByItemAndParticipants(item.getId(), currentUserId, otherUserId);
        if (existing.isPresent()) {
            return ConversationResponse.from(existing.get());
        }

        User currentUser =
                userRepository.findById(currentUserId).orElseThrow(() -> new NotFoundException("User not found"));
        User otherUser =
                userRepository.findById(otherUserId).orElseThrow(() -> new NotFoundException("Other user not found"));

        Conversation conversation = conversationRepository.save(new Conversation(item, currentUser, otherUser));
        return ConversationResponse.from(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listForUser(UUID userId, Pageable pageable) {
        return PageResponse.from(
                conversationRepository.findAllForUser(userId, pageable).map(ConversationResponse::from));
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
        return ConversationResponse.from(conversation);
    }
}
