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
    private final com.openforum.domain.repository.ThreadRepository threadRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            com.openforum.domain.repository.ThreadRepository threadRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.threadRepository = threadRepository;
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

    @Transactional(readOnly = true)
    public List<com.openforum.application.dto.SubscriptionWithThreadDto> getSubscriptionsForUser(String tenantId,
            UUID userId, int page, int size) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId, page, size);

        if (subscriptions.isEmpty()) {
            return List.of();
        }

        List<UUID> threadIds = subscriptions.stream()
                .map(Subscription::getTargetId)
                .collect(Collectors.toList());

        // Aggregate Stitching: Fetch thread titles
        // We use findAllByIds if available, or findById in loop (less efficient but
        // acceptable for small page sizes).
        // Checking ThreadRepository interface... assuming findById is available.
        // Ideally we should add findAllByIds to ThreadRepository for performance.
        // For now, let's loop since page size is small (e.g. 10-20).

        // Optimization: We can fetch all threads in one go if ThreadRepository supports
        // it.
        // Let's check ThreadRepository. It usually has findById.
        // I'll assume for now we iterate.

        return subscriptions.stream().map(sub -> {
            String title = threadRepository.findById(sub.getTargetId())
                    .map(com.openforum.domain.aggregate.Thread::getTitle)
                    .orElse("Unknown Thread");
            return new com.openforum.application.dto.SubscriptionWithThreadDto(
                    sub.getTargetId(),
                    title,
                    sub.getCreatedAt());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countSubscriptionsForUser(UUID userId) {
        return subscriptionRepository.countByUserId(userId);
    }
}
