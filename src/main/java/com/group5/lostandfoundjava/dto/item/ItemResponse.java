package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A full item, including a compact view of its reporter and its category. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {

    private UUID id;

    private ItemType type;

    private String name;

    private String description;

    private String brand;

    private String color;

    private List<String> photoUrls;

    private Double locationLat;

    private Double locationLng;

    private Instant dateTime;

    private ItemStatus status;

    private BigDecimal rewardAmount;

    private String storageLocation;

    private UserSummaryResponse owner;

    private CategoryResponse category;

    private Instant createdAt;
}
