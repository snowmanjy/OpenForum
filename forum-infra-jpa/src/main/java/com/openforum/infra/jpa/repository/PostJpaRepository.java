package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

@Repository
public interface PostJpaRepository extends JpaRepository<PostEntity, UUID> {
    Page<PostEntity> findByThreadId(UUID threadId, Pageable pageable);

    /**
     * Tenant-aware query for posts in a thread, sorted by post number
     * (chronological).
     */
    Page<PostEntity> findByThreadIdAndTenantIdOrderByPostNumberAsc(UUID threadId, String tenantId, Pageable pageable);

    @Override
    @Query("select p from PostEntity p where p.id = :id")
    Optional<PostEntity> findById(@Param("id") UUID id);

    Optional<PostEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<PostEntity> findByTenantId(String tenantId, Pageable pageable);
}
