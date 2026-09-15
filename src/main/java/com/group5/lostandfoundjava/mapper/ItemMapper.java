package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.ItemSummaryResponse;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates between {@link Item} and its DTOs.
 *
 * <p>The two relations an item has — its reporter and its category — cannot be resolved from a
 * request on their own, because turning an id into an entity needs a repository and a "not found"
 * decision. So the service looks them up and hands them in already resolved, and this class stays a
 * pure field-copier with no database access of its own.
 *
 * <p>The same division applies to the rules about which fields belong on a LOST versus a FOUND item:
 * those are validated in the service before anything gets copied here.
 */
@Component
@RequiredArgsConstructor
public class ItemMapper {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public Item toEntity(CreateItemRequest request, User owner, Category category) {
        if (request == null) {
            return null;
        }
        Item item = new Item(owner, category, request.getType(), trimmed(request.getName()));
        item.setDescription(request.getDescription());
        item.setBrand(request.getBrand());
        item.setColor(request.getColor());
        if (request.getPhotoUrls() != null) {
            item.getPhotoUrls().addAll(request.getPhotoUrls());
        }
        item.setLocationLat(request.getLocationLat());
        item.setLocationLng(request.getLocationLng());
        item.setDateTime(request.getDateTime());
        item.setRewardAmount(request.getRewardAmount());
        item.setStorageLocation(request.getStorageLocation());
        return item;
    }

    /**
     * Copies a partial update onto an existing item. Every {@code null} field is skipped.
     *
     * @param category the already-resolved replacement category, or {@code null} to keep the
     *     current one
     */
    public void updateEntity(Item item, UpdateItemRequest request, Category category) {
        if (item == null || request == null) {
            return;
        }
        if (request.getName() != null) {
            item.setName(request.getName().trim());
        }
        if (category != null) {
            item.setCategory(category);
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getBrand() != null) {
            item.setBrand(request.getBrand());
        }
        if (request.getColor() != null) {
            item.setColor(request.getColor());
        }
        if (request.getPhotoUrls() != null) {
            // Replace rather than append: the client always sends the complete list.
            item.getPhotoUrls().clear();
            item.getPhotoUrls().addAll(request.getPhotoUrls());
        }
        if (request.getLocationLat() != null) {
            item.setLocationLat(request.getLocationLat());
        }
        if (request.getLocationLng() != null) {
            item.setLocationLng(request.getLocationLng());
        }
        if (request.getDateTime() != null) {
            item.setDateTime(request.getDateTime());
        }
        if (request.getRewardAmount() != null) {
            item.setRewardAmount(request.getRewardAmount());
        }
        if (request.getStorageLocation() != null) {
            item.setStorageLocation(request.getStorageLocation());
        }
    }

    public ItemResponse toResponse(Item item) {
        if (item == null) {
            return null;
        }
        return ItemResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .name(item.getName())
                .description(item.getDescription())
                .brand(item.getBrand())
                .color(item.getColor())
                // Copied so the response cannot be used to reach back into the managed entity.
                .photoUrls(List.copyOf(item.getPhotoUrls()))
                .locationLat(item.getLocationLat())
                .locationLng(item.getLocationLng())
                .dateTime(item.getDateTime())
                .status(item.getStatus())
                .rewardAmount(item.getRewardAmount())
                .storageLocation(item.getStorageLocation())
                .owner(userMapper.toSummaryResponse(item.getUser()))
                .category(categoryMapper.toResponse(item.getCategory()))
                .createdAt(item.getCreatedAt())
                .build();
    }

    public ItemSummaryResponse toSummaryResponse(Item item) {
        if (item == null) {
            return null;
        }
        return ItemSummaryResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .type(item.getType())
                .status(item.getStatus())
                .build();
    }

    public List<ItemResponse> toResponseList(List<Item> items) {
        if (items == null) {
            return List.of();
        }
        List<ItemResponse> responses = new ArrayList<>(items.size());
        items.forEach(item -> responses.add(toResponse(item)));
        return responses;
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
