package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Subscription;
import com.openforum.domain.repository.SubscriptionRepository;
import com.openforum.infra.jpa.entity.SubscriptionEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepository;

    public SubscriptionRepositoryImpl(SubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(subscription.getId());
        entity.setTenantId(subscription.getTenantId());
        entity.setUserId(subscription.getUserId());
        entity.setTargetId(subscription.getTargetId());
        entity.setTargetType(subscription.getTargetType());
        entity.setCreatedAt(subscription.getCreatedAt());
        jpaRepository.save(entity);
    }

    @Override
    public void delete(String tenantId, UUID userId, UUID targetId) {
        jpaRepository.deleteByTenantIdAndUserIdAndTargetId(tenantId, userId, targetId);
    }

    @Override
    public List<Subscription> findByTarget(UUID targetId) {
        return jpaRepository.findByTargetId(targetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(UUID userId, UUID targetId) {
        return jpaRepository.existsByUserIdAndTargetId(userId, targetId);
    }

    @Override
    public List<Subscription> findByUserId(UUID userId, int page, int size) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page,
                size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return jpaRepository.findByUserId(userId, pageRequest).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        return Subscription.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getTargetId(),
                entity.getTargetType(),
                entity.getCreatedAt());
    }
}
