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
    private final com.openforum.domain.repository.CategoryRepository categoryRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            com.openforum.domain.repository.ThreadRepository threadRepository,
            com.openforum.domain.repository.CategoryRepository categoryRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.threadRepository = threadRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void subscribe(String tenantId, UUID userId, UUID targetId, TargetType targetType) {
        if (subscriptionRepository.exists(userId, targetId)) {
            return;
        }

        // Validation
        if (targetType == TargetType.THREAD) {
            if (threadRepository.findById(targetId).isEmpty()) {
                throw new IllegalArgumentException("Thread not found: " + targetId);
            }
        } else if (targetType == TargetType.CATEGORY) {
            if (categoryRepository.findById(targetId).isEmpty()) {
                throw new IllegalArgumentException("Category not found: " + targetId);
            }
        }

        Subscription subscription = Subscription.create(tenantId, userId, targetId, targetType);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String tenantId, UUID userId, UUID targetId) {
        subscriptionRepository.delete(tenantId, userId, targetId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getSubscribers(UUID targetId) {
        return subscriptionRepository.findByTarget(targetId).stream()
                .map(Subscription::getUserId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<com.openforum.application.dto.SubscriptionDto> getSubscriptionsForUser(String tenantId,
            UUID userId, int page, int size) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId, page, size);

        if (subscriptions.isEmpty()) {
            return List.of();
        }

        return subscriptions.stream().map(sub -> {
            String title = "Unknown";
            if (sub.getTargetType() == TargetType.THREAD) {
                title = threadRepository.findById(sub.getTargetId())
                        .map(com.openforum.domain.aggregate.Thread::getTitle)
                        .orElse("Unknown Thread");
            } else if (sub.getTargetType() == TargetType.CATEGORY) {
                title = categoryRepository.findById(sub.getTargetId())
                        .map(com.openforum.domain.aggregate.Category::getName)
                        .orElse("Unknown Category");
            }
            return new com.openforum.application.dto.SubscriptionDto(
                    sub.getTargetId(),
                    sub.getTargetType(),
                    title,
                    sub.getCreatedAt());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countSubscriptionsForUser(UUID userId) {
        return subscriptionRepository.countByUserId(userId);
    }
}
