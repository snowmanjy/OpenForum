package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {
    List<SubscriptionEntity> findByTargetId(UUID targetId);

    boolean existsByUserIdAndTargetId(UUID userId, UUID targetId);

    void deleteByTenantIdAndUserIdAndTargetId(String tenantId, UUID userId, UUID targetId);
}
