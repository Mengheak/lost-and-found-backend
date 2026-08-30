package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.ItemSearchFilter;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Reporting, searching and managing lost or found items. */
public interface ItemService {

    ItemResponse create(UUID userId, CreateItemRequest request);

    ItemResponse get(UUID itemId);

    /** Only the reporter may edit; anyone else gets a 403. */
    ItemResponse update(UUID userId, UUID itemId, UpdateItemRequest request);

    void delete(UUID userId, UUID itemId);

    PageResponse<ItemResponse> search(ItemSearchFilter filter, Pageable pageable);

    PageResponse<ItemResponse> listOwn(UUID userId, Pageable pageable);

    ItemResponse updateStatus(UUID userId, UUID itemId, ItemStatus status);
}
