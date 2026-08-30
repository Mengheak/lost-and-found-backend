package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemStatus;

public record UpdateItemStatusRequest(ItemStatus status) {}
