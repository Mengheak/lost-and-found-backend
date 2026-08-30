package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import java.util.UUID;

/** The compact view embedded in a conversation, so a chat list can show what it is about. */
public record ItemSummaryResponse(UUID id, String name, ItemType type, ItemStatus status) {

    public static ItemSummaryResponse from(Item item) {
        return new ItemSummaryResponse(
                item.getId(), item.getName(), item.getType(), item.getStatus());
    }
}
