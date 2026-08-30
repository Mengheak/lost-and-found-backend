package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.time.Instant;
import java.util.UUID;


public record ItemSearchFilter(
        ItemType type,
        ItemStatus status,
        UUID categoryId,
        String keyword,
        String brand,
        String color,
        Instant dateFrom,
        Instant dateTo) {}
