package com.openforum.application.service;

import com.openforum.domain.aggregate.Subscription;
import com.openforum.domain.repository.SubscriptionRepository;
import com.openforum.domain.valueobject.TargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public void subscribe(String tenantId, UUID userId, UUID threadId) {
        if (subscriptionRepository.exists(userId, threadId)) {
            // Idempotent: already subscribed, do nothing
            return;
        }

        Subscription subscription = Subscription.create(tenantId, userId, threadId, TargetType.THREAD);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String tenantId, UUID userId, UUID threadId) {
        subscriptionRepository.delete(tenantId, userId, threadId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getSubscribers(UUID threadId) {
        return subscriptionRepository.findByTarget(threadId).stream()
                .map(Subscription::getUserId)
                .collect(Collectors.toList());
    }
}
