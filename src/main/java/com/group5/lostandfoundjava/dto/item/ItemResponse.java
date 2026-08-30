package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A full item, including a compact view of its reporter and its category. */
public record ItemResponse(
        UUID id,
        ItemType type,
        String name,
        String description,
        String brand,
        String color,
        List<String> photoUrls,
        Double locationLat,
        Double locationLng,
        Instant dateTime,
        ItemStatus status,
        BigDecimal rewardAmount,
        String storageLocation,
        UserSummaryResponse owner,
        CategoryResponse category,
        Instant createdAt) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getType(),
                item.getName(),
                item.getDescription(),
                item.getBrand(),
                item.getColor(),
                List.copyOf(item.getPhotoUrls()),
                item.getLocationLat(),
                item.getLocationLng(),
                item.getDateTime(),
                item.getStatus(),
                item.getRewardAmount(),
                item.getStorageLocation(),
                UserSummaryResponse.from(item.getUser()),
                CategoryResponse.from(item.getCategory()),
                item.getCreatedAt());
    }
}
