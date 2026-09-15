package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.ForbiddenException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.mapper.CategoryMapper;
import com.group5.lostandfoundjava.mapper.ItemMapper;
import com.group5.lostandfoundjava.mapper.UserMapper;
import com.group5.lostandfoundjava.repository.CategoryRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Item creation rules and ownership checks
class ItemServiceImplTest {

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ItemServiceImpl service =
            new ItemServiceImpl(
                    itemRepository,
                    categoryRepository,
                    userRepository,
                    new ItemMapper(new UserMapper(), new CategoryMapper()));

    private final User owner = new User("Owner", "owner@example.com", null, "hash", Role.USER);
    private final Category category = new Category("Wallet", null);

    @Test
    @DisplayName("create maps the request onto a new OPEN item")
    void createMapsRequestOntoOpenItem() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = service.create(
                owner.getId(),
                request(ItemType.LOST, "  Black leather wallet  ", "Fossil", "black", new BigDecimal("25.00"), null));

        assertEquals("Black leather wallet", response.getName());
        assertEquals(ItemType.LOST, response.getType());
        assertEquals(ItemStatus.OPEN, response.getStatus());
        assertEquals(new BigDecimal("25.00"), response.getRewardAmount());
        assertEquals(owner.getId(), response.getOwner().getId());
        assertEquals(category.getId(), response.getCategory().getId());
    }

    @Test
    @DisplayName("create rejects a reward on a FOUND item")
    void createRejectsRewardOnFoundItem() {
        assertThrows(
                BadRequestException.class,
                () -> service.create(
                        owner.getId(), request(ItemType.FOUND, "Wallet", null, null, BigDecimal.TEN, null)));
    }

    @Test
    @DisplayName("create rejects a storage location on a LOST item")
    void createRejectsStorageLocationOnLostItem() {
        assertThrows(
                BadRequestException.class,
                () -> service.create(
                        owner.getId(), request(ItemType.LOST, "Wallet", null, null, null, "Front desk")));
    }

    @Test
    @DisplayName("get throws NotFoundException for an unknown item")
    void getThrowsForUnknownItem() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(id));
    }

    @Test
    @DisplayName("update by a non-owner throws ForbiddenException")
    void updateByNonOwnerThrows() {
        Item item = lostItem();
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        UpdateItemRequest update =
                new UpdateItemRequest("New name", null, null, null, null, null, null, null, null, null, null);

        assertThrows(
                ForbiddenException.class, () -> service.update(UUID.randomUUID(), item.getId(), update));
    }

    @Test
    @DisplayName("updateStatus by the owner changes the status")
    void updateStatusByOwnerChangesStatus() {
        Item item = lostItem();
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = service.updateStatus(owner.getId(), item.getId(), ItemStatus.RETURNED);

        assertEquals(ItemStatus.RETURNED, response.getStatus());
    }

    // Builds a create request, keeping the tests above free of a dozen nulls each
    private CreateItemRequest request(
            ItemType type,
            String name,
            String brand,
            String color,
            BigDecimal rewardAmount,
            String storageLocation) {
        return new CreateItemRequest(
                type,
                name,
                category.getId(),
                null,
                brand,
                color,
                null,
                null,
                null,
                null,
                rewardAmount,
                storageLocation);
    }

    private Item lostItem() {
        return new Item(owner, category, ItemType.LOST, "Black wallet");
    }
}
