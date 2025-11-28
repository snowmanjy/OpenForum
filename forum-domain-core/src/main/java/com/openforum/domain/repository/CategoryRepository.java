package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAll(String tenantId);

    Optional<Category> findBySlug(String tenantId, String slug);
}
