package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.dto.category.CategoryRequest;
import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import java.util.List;
import java.util.UUID;

// The item taxonomy
public interface CategoryService {

    List<CategoryResponse> list();

    CategoryResponse get(UUID id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    void delete(UUID id);
}
