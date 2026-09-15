package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.category.CategoryRequest;
import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import com.group5.lostandfoundjava.entity.Category;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) {
            return null;
        }
        return new Category(trimmed(request.getName()), request.getIconUrl());
    }

    // Both fields are always sent on a category, so this is a full overwrite, not a patch
    public void updateEntity(Category category, CategoryRequest request) {
        if (category == null || request == null) {
            return;
        }
        category.setName(trimmed(request.getName()));
        category.setIconUrl(request.getIconUrl());
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .iconUrl(category.getIconUrl())
                .build();
    }

    public List<CategoryResponse> toResponseList(List<Category> categories) {
        if (categories == null) {
            return List.of();
        }
        List<CategoryResponse> responses = new ArrayList<>(categories.size());
        categories.forEach(category -> responses.add(toResponse(category)));
        return responses;
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
