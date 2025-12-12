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
        entity.setMemberId(subscription.getMemberId());
        entity.setTargetId(subscription.getTargetId());
        entity.setTargetType(subscription.getTargetType());
        entity.setCreatedAt(subscription.getCreatedAt());
        entity.setCreatedBy(subscription.getCreatedBy());
        entity.setLastModifiedAt(subscription.getLastModifiedAt());
        entity.setLastModifiedBy(subscription.getLastModifiedBy());
        jpaRepository.save(entity);
    }

    @Override
    public void delete(String tenantId, UUID memberId, UUID targetId) {
        jpaRepository.deleteByTenantIdAndMemberIdAndTargetId(tenantId, memberId, targetId);
    }

    @Override
    public List<Subscription> findByTarget(UUID targetId) {
        return jpaRepository.findByTargetId(targetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(UUID memberId, UUID targetId) {
        return jpaRepository.existsByMemberIdAndTargetId(memberId, targetId);
    }

    @Override
    public List<Subscription> findByMemberId(UUID memberId, int page, int size) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(page,
                size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return jpaRepository.findByMemberId(memberId, pageRequest).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByMemberId(UUID memberId) {
        return jpaRepository.countByMemberId(memberId);
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        return Subscription.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getMemberId(),
                entity.getTargetId(),
                entity.getTargetType(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedAt(),
                entity.getLastModifiedBy());
    }
}
