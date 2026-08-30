package com.group5.lostandfoundjava.dto.category;

import com.group5.lostandfoundjava.entity.Category;
import java.util.UUID;

/** A category as the client sees it. */
public record CategoryResponse(UUID id, String name, String iconUrl) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getIconUrl());
    }
}
