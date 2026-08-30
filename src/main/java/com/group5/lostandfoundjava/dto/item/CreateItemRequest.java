package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /api/items}.
 *
 * <p>{@code rewardAmount} is only accepted on {@code LOST} items and {@code storageLocation} only
 * on {@code FOUND} ones. That rule depends on another field, so it lives in the service rather than
 * in an annotation.
 */
public record CreateItemRequest(
        ItemType type,
        @NotBlank @Size(max = 255) String name,
        UUID categoryId,
        @Size(max = 10_000) String description,
        @Size(max = 100) String brand,
        @Size(max = 50) String color,
        @Size(max = 10) List<String> photoUrls,
        Double locationLat,
        Double locationLng,
        Instant dateTime,
        @PositiveOrZero BigDecimal rewardAmount,
        @Size(max = 255) String storageLocation) {}
