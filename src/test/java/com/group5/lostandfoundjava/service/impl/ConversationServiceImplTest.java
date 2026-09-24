package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.dto.chat.StartConversationRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Conversation;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.mapper.ConversationMapper;
import com.group5.lostandfoundjava.mapper.ItemMapper;
import com.group5.lostandfoundjava.mapper.CategoryMapper;
import com.group5.lostandfoundjava.mapper.UserMapper;
import com.group5.lostandfoundjava.repository.ConversationRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConversationServiceImplTest {

    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final ItemRepository items = mock(ItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserMapper userMapper = new UserMapper();
    private final ConversationServiceImpl service = new ConversationServiceImpl(
            conversations,
            items,
            users,
            new ConversationMapper(new ItemMapper(userMapper, new CategoryMapper()), userMapper));

    private final User publisher = new User("Publisher", "publisher@example.com", null, "hash", Role.USER);
    private final User contact = new User("Contact", "contact@example.com", null, "hash", Role.USER);
    private final Item item = new Item(publisher, new Category("Wallet", null), ItemType.FOUND, "Wallet");

    @Test
    void newConversationAlwaysIncludesItemPublisher() {
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        when(users.findById(contact.getId())).thenReturn(Optional.of(contact));
        when(conversations.findByItemAndParticipants(item.getId(), contact.getId(), publisher.getId()))
                .thenReturn(Optional.empty());
        when(conversations.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.startOrGet(contact.getId(), new StartConversationRequest(item.getId()));

        assertEquals(contact.getId(), response.getUserA().getId());
        assertEquals(publisher.getId(), response.getUserB().getId());
        verify(conversations).findByItemAndParticipants(item.getId(), contact.getId(), publisher.getId());
        verify(users, never()).findById(publisher.getId());
    }

    @Test
    void existingConversationIsReturnedForSameItemAndContact() {
        Conversation existing = new Conversation(item, publisher, contact);
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        when(conversations.findByItemAndParticipants(item.getId(), contact.getId(), publisher.getId()))
                .thenReturn(Optional.of(existing));

        var response = service.startOrGet(contact.getId(), new StartConversationRequest(item.getId()));

        assertEquals(existing.getId(), response.getId());
        verify(conversations, never()).save(any());
    }

    @Test
    void publisherCannotStartConversationWithThemselves() {
        when(items.findById(item.getId())).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class,
                () -> service.startOrGet(publisher.getId(), new StartConversationRequest(item.getId())));

        verify(conversations, never()).save(any());
    }
}
