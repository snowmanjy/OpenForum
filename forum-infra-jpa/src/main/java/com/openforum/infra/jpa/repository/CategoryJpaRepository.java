package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findAllByTenantId(String tenantId);

    Optional<CategoryEntity> findByTenantIdAndSlug(String tenantId, String slug);
}
