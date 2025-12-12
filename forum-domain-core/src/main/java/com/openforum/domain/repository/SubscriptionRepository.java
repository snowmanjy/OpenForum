package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Subscription;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository {
    void save(Subscription subscription);

    void delete(String tenantId, UUID memberId, UUID targetId);

    List<Subscription> findByTarget(UUID targetId);

    boolean exists(UUID memberId, UUID targetId);

    List<Subscription> findByMemberId(UUID memberId, int page, int size);

    long countByMemberId(UUID memberId);
}
