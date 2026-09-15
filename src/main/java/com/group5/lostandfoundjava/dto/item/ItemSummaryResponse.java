package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Just enough of an item to label a conversation with it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSummaryResponse {

    private UUID id;

    private String name;

    private ItemType type;

    private ItemStatus status;
}
