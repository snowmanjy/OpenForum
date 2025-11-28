package com.openforum.application.service;

import com.openforum.domain.aggregate.Category;
import com.openforum.domain.aggregate.CategoryFactory;
import com.openforum.domain.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category createCategory(String tenantId, String name, String slug, String description, boolean isReadOnly) {
        if (categoryRepository.findBySlug(tenantId, slug).isPresent()) {
            throw new IllegalArgumentException("Category with slug '" + slug + "' already exists for this tenant.");
        }
        Category category = CategoryFactory.create(tenantId, name, slug, description, isReadOnly);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(String tenantId) {
        return categoryRepository.findAll(tenantId);
    }
}
