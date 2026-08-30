package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.saveditem.SavedItemResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** A user's personal shortlist of items. */
public interface SavedItemService {

    /** Idempotent: saving an item that is already saved returns the existing row. */
    SavedItemResponse save(UUID userId, UUID itemId);

    void unsave(UUID userId, UUID itemId);

    PageResponse<SavedItemResponse> list(UUID userId, Pageable pageable);
}
