package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.ItemSearchFilter;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.exception.BadRequestException;
import com.group5.lostandfoundjava.exception.ForbiddenException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.mapper.ItemMapper;
import com.group5.lostandfoundjava.repository.CategoryRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.repository.specification.ItemSpecifications;
import com.group5.lostandfoundjava.service.ItemService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponse create(UUID userId, CreateItemRequest request) {
        // Checked before anything is loaded, so a bad request costs no database round trips.
        if (request.getType() == ItemType.FOUND && request.getRewardAmount() != null) {
            throw new BadRequestException("rewardAmount is only allowed for LOST items");
        }
        if (request.getType() == ItemType.LOST && request.getStorageLocation() != null) {
            throw new BadRequestException("storageLocation is only allowed for FOUND items");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Category category = findCategory(request.getCategoryId());

        return itemMapper.toResponse(itemRepository.save(itemMapper.toEntity(request, user, category)));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse get(UUID itemId) {
        return itemMapper.toResponse(findItem(itemId));
    }

    // Every null field is skipped, so a client can send only what actually changed
    @Override
    @Transactional
    public ItemResponse update(UUID userId, UUID itemId, UpdateItemRequest request) {
        Item item = findOwnedItem(userId, itemId);

        // The rules below decide what a caller may ask for; the mapper only copies what survives them
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }
        if (request.getRewardAmount() != null && item.getType() == ItemType.FOUND) {
            throw new BadRequestException("rewardAmount is only allowed for LOST items");
        }
        if (request.getStorageLocation() != null && item.getType() == ItemType.LOST) {
            throw new BadRequestException("storageLocation is only allowed for FOUND items");
        }

        // Resolved here because turning an id into an entity needs a repository and a 404 decision.
        Category category = request.getCategoryId() == null ? null : findCategory(request.getCategoryId());

        itemMapper.updateEntity(item, request, category);

        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID itemId) {
        itemRepository.delete(findOwnedItem(userId, itemId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> search(ItemSearchFilter filter, Pageable pageable) {
        return PageResponse.from(itemRepository
                .findAll(ItemSpecifications.matching(filter), pageable)
                .map(itemMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> listOwn(UUID userId, Pageable pageable) {
        return PageResponse.from(itemRepository.findByUserId(userId, pageable).map(itemMapper::toResponse));
    }

    @Override
    @Transactional
    public ItemResponse updateStatus(UUID userId, UUID itemId, ItemStatus status) {
        Item item = findOwnedItem(userId, itemId);
        item.setStatus(status);
        return itemMapper.toResponse(itemRepository.save(item));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private Item findItem(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item not found"));
    }

    // Loads an item and refuses unless the caller reported
    private Item findOwnedItem(UUID userId, UUID itemId) {
        Item item = findItem(itemId);
        if (!item.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not the owner of this item");
        }
        return item;
    }
}
