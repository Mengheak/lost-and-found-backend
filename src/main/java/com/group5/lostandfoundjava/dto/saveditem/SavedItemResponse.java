package com.group5.lostandfoundjava.dto.saveditem;

import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.entity.SavedItem;
import java.time.Instant;
import java.util.UUID;

/** One row of the caller's shortlist, with the whole item embedded so no second call is needed. */
public record SavedItemResponse(UUID id, ItemResponse item, Instant savedAt) {

    public static SavedItemResponse from(SavedItem savedItem) {
        return new SavedItemResponse(
                savedItem.getId(), ItemResponse.from(savedItem.getItem()), savedItem.getCreatedAt());
    }
}
