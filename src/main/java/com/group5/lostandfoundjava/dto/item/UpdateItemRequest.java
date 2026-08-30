package com.group5.lostandfoundjava.dto.item;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateItemRequest(
        @Size(min = 1, max = 255) String name,
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
