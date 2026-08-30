package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.exception.ConflictException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.category.CategoryRequest;
import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.repository.CategoryRepository;
import com.group5.lostandfoundjava.service.CategoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Not paged: there are a dozen categories and clients want all of them for a dropdown. */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return CategoryResponse.from(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category with this name already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name, request.iconUrl())));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findCategory(id);
        String newName = request.name().trim();

        // Renaming to a different spelling of its own name is allowed; clashing with another is not.
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new ConflictException("Category with this name already exists");
        }

        category.setName(newName);
        category.setIconUrl(request.iconUrl());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /**
     * Deleting a category that items still reference is refused by the database's foreign key, which
     * the exception handler reports as a 409 conflict.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        categoryRepository.delete(findCategory(id));
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }
}
