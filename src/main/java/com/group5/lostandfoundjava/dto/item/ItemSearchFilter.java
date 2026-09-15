package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The item search's query parameters, gathered into one object
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSearchFilter {

    private ItemType type;

    private ItemStatus status;

    private UUID categoryId;

    // Free text matched against both the item's name and its description
    private String keyword;

    private String brand;

    private String color;

    private Instant dateFrom;

    private Instant dateTo;
}
