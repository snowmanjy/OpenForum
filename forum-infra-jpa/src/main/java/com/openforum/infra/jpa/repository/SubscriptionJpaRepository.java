package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.SubscriptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {
    List<SubscriptionEntity> findByTargetId(UUID targetId);

    boolean existsByMemberIdAndTargetId(UUID memberId, UUID targetId);

    void deleteByTenantIdAndMemberIdAndTargetId(String tenantId, UUID memberId, UUID targetId);

    Page<SubscriptionEntity> findByMemberId(UUID memberId, Pageable pageable);

    long countByMemberId(UUID memberId);
}
