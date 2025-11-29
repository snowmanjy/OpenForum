package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.TagEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagJpaRepository extends JpaRepository<TagEntity, UUID> {
    Optional<TagEntity> findByTenantIdAndName(String tenantId, String name);

    @Query("SELECT t FROM TagEntity t WHERE t.tenantId = :tenantId AND t.name LIKE :prefix%")
    List<TagEntity> findByNameStartingWith(@Param("tenantId") String tenantId, @Param("prefix") String prefix,
            Pageable pageable);

    @Modifying
    @Query("UPDATE TagEntity t SET t.usageCount = t.usageCount + 1 WHERE t.tenantId = :tenantId AND t.name = :name")
    void incrementUsageCount(@Param("tenantId") String tenantId, @Param("name") String name);
}
