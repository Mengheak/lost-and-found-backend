package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.ForbiddenException;
import com.group5.lostandfoundjava.common.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.ItemSearchFilter;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.repository.CategoryRepository;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.repository.specification.ItemSpecifications;
import com.group5.lostandfoundjava.service.ItemService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ItemServiceImpl(
            ItemRepository itemRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ItemResponse create(UUID userId, CreateItemRequest request) {
        // Checked before anything is loaded, so a bad request costs no database round trips.
        if (request.type() == ItemType.FOUND && request.rewardAmount() != null) {
            throw new BadRequestException("rewardAmount is only allowed for LOST items");
        }
        if (request.type() == ItemType.LOST && request.storageLocation() != null) {
            throw new BadRequestException("storageLocation is only allowed for FOUND items");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Item item = new Item(user, category, request.type(), request.name().trim());
        item.setDescription(request.description());
        item.setBrand(request.brand());
        item.setColor(request.color());
        if (request.photoUrls() != null) {
            item.getPhotoUrls().addAll(request.photoUrls());
        }
        item.setLocationLat(request.locationLat());
        item.setLocationLng(request.locationLng());
        item.setDateTime(request.dateTime());
        item.setRewardAmount(request.rewardAmount());
        item.setStorageLocation(request.storageLocation());

        return ItemResponse.from(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse get(UUID itemId) {
        return ItemResponse.from(findItem(itemId));
    }

    /** Every {@code null} field is skipped, so a client can send only what actually changed. */
    @Override
    @Transactional
    public ItemResponse update(UUID userId, UUID itemId, UpdateItemRequest request) {
        Item item = findOwnedItem(userId, itemId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Name must not be blank");
            }
            item.setName(request.name().trim());
        }
        if (request.categoryId() != null) {
            item.setCategory(categoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found")));
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.brand() != null) {
            item.setBrand(request.brand());
        }
        if (request.color() != null) {
            item.setColor(request.color());
        }
        if (request.photoUrls() != null) {
            // Replace rather than append: the client always sends the complete list.
            item.getPhotoUrls().clear();
            item.getPhotoUrls().addAll(request.photoUrls());
        }
        if (request.locationLat() != null) {
            item.setLocationLat(request.locationLat());
        }
        if (request.locationLng() != null) {
            item.setLocationLng(request.locationLng());
        }
        if (request.dateTime() != null) {
            item.setDateTime(request.dateTime());
        }
        if (request.rewardAmount() != null) {
            if (item.getType() == ItemType.FOUND) {
                throw new BadRequestException("rewardAmount is only allowed for LOST items");
            }
            item.setRewardAmount(request.rewardAmount());
        }
        if (request.storageLocation() != null) {
            if (item.getType() == ItemType.LOST) {
                throw new BadRequestException("storageLocation is only allowed for FOUND items");
            }
            item.setStorageLocation(request.storageLocation());
        }

        return ItemResponse.from(itemRepository.save(item));
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
                .map(ItemResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> listOwn(UUID userId, Pageable pageable) {
        return PageResponse.from(itemRepository.findByUserId(userId, pageable).map(ItemResponse::from));
    }

    @Override
    @Transactional
    public ItemResponse updateStatus(UUID userId, UUID itemId, ItemStatus status) {
        Item item = findOwnedItem(userId, itemId);
        item.setStatus(status);
        return ItemResponse.from(itemRepository.save(item));
    }

    private Item findItem(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item not found"));
    }

    /**
     * Loads an item and refuses unless the caller reported it. Ownership is checked here, in one
     * place, instead of being repeated in every method that changes an item.
     */
    private Item findOwnedItem(UUID userId, UUID itemId) {
        Item item = findItem(itemId);
        if (!item.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not the owner of this item");
        }
        return item;
    }
}
