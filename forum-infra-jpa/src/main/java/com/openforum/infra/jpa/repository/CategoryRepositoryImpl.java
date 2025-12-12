package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Category;
import com.openforum.domain.repository.CategoryRepository;
import com.openforum.infra.jpa.entity.CategoryEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = toEntity(category);
        CategoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Category> findAll(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findBySlug(String tenantId, String slug) {
        return jpaRepository.findByTenantIdAndSlug(tenantId, slug).map(this::toDomain);
    }

    private CategoryEntity toEntity(Category domain) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setName(domain.getName());
        entity.setSlug(domain.getSlug());
        entity.setDescription(domain.getDescription());
        entity.setReadOnly(domain.isReadOnly());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getCreatedBy() != null) {
            entity.setCreatedBy(domain.getCreatedBy());
        }
        if (domain.getLastModifiedAt() != null) {
            entity.setLastModifiedAt(domain.getLastModifiedAt());
        }
        if (domain.getLastModifiedBy() != null) {
            entity.setLastModifiedBy(domain.getLastModifiedBy());
        }
        return entity;
    }

    private Category toDomain(CategoryEntity entity) {
        return Category.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isReadOnly(),
                entity.getCreatedAt(),
                entity.getLastModifiedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy());
    }
}
