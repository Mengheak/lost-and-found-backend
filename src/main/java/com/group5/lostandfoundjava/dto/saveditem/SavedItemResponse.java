package com.group5.lostandfoundjava.dto.saveditem;

import com.group5.lostandfoundjava.dto.item.ItemResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry of a user's shortlist, with the whole item embedded. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedItemResponse {

    private UUID id;

    private ItemResponse item;

    private Instant savedAt;
}
