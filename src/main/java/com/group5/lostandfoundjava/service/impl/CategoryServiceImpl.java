package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.dto.category.CategoryRequest;
import com.group5.lostandfoundjava.dto.category.CategoryResponse;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.exception.ConflictException;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.mapper.CategoryMapper;
import com.group5.lostandfoundjava.repository.CategoryRepository;
import com.group5.lostandfoundjava.service.CategoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryMapper.toResponseList(categoryRepository.findAll(Sort.by("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);

        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new ConflictException("Category with this name already exists");
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findCategory(id);
        String newName = request.getName() == null ? null : request.getName().trim();

        // Renaming to a different spelling of its own name is allowed; clashing with another is not.
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new ConflictException("Category with this name already exists");
        }

        categoryMapper.updateEntity(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
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
