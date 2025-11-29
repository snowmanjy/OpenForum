package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PrivateThreadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PrivateThreadJpaRepository extends JpaRepository<PrivateThreadEntity, UUID> {

    @Query("SELECT t FROM PrivateThreadEntity t JOIN t.participantIds p WHERE t.tenantId = :tenantId AND p = :participantId ORDER BY t.lastActivityAt DESC")
    Page<PrivateThreadEntity> findByTenantIdAndParticipantId(@Param("tenantId") String tenantId,
            @Param("participantId") UUID participantId, Pageable pageable);
}
