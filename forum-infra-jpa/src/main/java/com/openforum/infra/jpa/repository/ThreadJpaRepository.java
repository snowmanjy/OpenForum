package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.ThreadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreadJpaRepository extends JpaRepository<ThreadEntity, UUID> {
    @Query(value = "SELECT * FROM threads WHERE tenant_id = :tenantId AND search_vector @@ plainto_tsquery('english', :query)", nativeQuery = true)
    Page<ThreadEntity> search(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

    Optional<ThreadEntity> findByIdAndTenantId(UUID id, String tenantId);

    Page<ThreadEntity> findByTenantId(String tenantId, Pageable pageable);

    @Override
    @Query("select t from ThreadEntity t where t.id = :id")
    Optional<ThreadEntity> findById(@Param("id") UUID id);
}
