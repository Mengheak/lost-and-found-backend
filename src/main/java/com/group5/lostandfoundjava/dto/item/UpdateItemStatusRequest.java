package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;

/** Body of {@code PATCH /api/items/{id}/status}. */
public record UpdateItemStatusRequest(ItemStatus status) {}
