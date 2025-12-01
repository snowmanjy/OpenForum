package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.MemberEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, UUID> {
    Optional<MemberEntity> findByTenantIdAndExternalId(String tenantId, String externalId);

    @Query("SELECT m FROM MemberEntity m WHERE (m.tenantId = :tenantId OR (:tenantId IS NULL AND m.tenantId IS NULL)) AND (LOWER(m.name) LIKE LOWER(:query) OR LOWER(m.email) LIKE LOWER(:query))")
    List<MemberEntity> searchByHandleOrName(@Param("tenantId") String tenantId, @Param("query") String query,
            Pageable pageable);

    long countByIdIn(List<UUID> ids);

    List<MemberEntity> findAllByExternalIdIn(List<String> externalIds);
}
