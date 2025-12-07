package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Subscription;
import com.openforum.domain.repository.SubscriptionRepository;
import com.openforum.domain.valueobject.TargetType;
import com.openforum.infra.jpa.config.JpaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({ SubscriptionRepositoryImpl.class, JpaTestConfig.class })
class SubscriptionRepositoryImplTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    void should_save_and_retrieve_subscription() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Subscription subscription = Subscription.create("tenant-1", userId, targetId, TargetType.THREAD);

        // When
        subscriptionRepository.save(subscription);

        // Then
        boolean exists = subscriptionRepository.exists(userId, targetId);
        assertThat(exists).isTrue();

        List<Subscription> subscriptions = subscriptionRepository.findByTarget(targetId);
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void should_prevent_duplicate_subscriptions() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Subscription sub1 = Subscription.create("tenant-1", userId, targetId, TargetType.THREAD);
        Subscription sub2 = Subscription.create("tenant-1", userId, targetId, TargetType.THREAD);

        // When
        subscriptionRepository.save(sub1);

        // Then
        assertThatThrownBy(() -> {
            subscriptionRepository.save(sub2);
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void should_delete_subscription() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String tenantId = "tenant-1";
        Subscription subscription = Subscription.create(tenantId, userId, targetId, TargetType.THREAD);
        subscriptionRepository.save(subscription);

        // When
        subscriptionRepository.delete(tenantId, userId, targetId);

        // Then
        boolean exists = subscriptionRepository.exists(userId, targetId);
        assertThat(exists).isFalse();
    }

    @Test
    void should_find_subscriptions_by_user_id_paginated() {
        // Given
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            Subscription sub = Subscription.create("tenant-1", userId, UUID.randomUUID(), TargetType.THREAD);
            subscriptionRepository.save(sub);
        }

        // When
        List<Subscription> page1 = subscriptionRepository.findByUserId(userId, 0, 3);
        List<Subscription> page2 = subscriptionRepository.findByUserId(userId, 1, 3);

        // Then
        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(2);
    }

    @Test
    void should_count_subscriptions_by_user_id() {
        // Given
        UUID userId = UUID.randomUUID();
        subscriptionRepository.save(Subscription.create("tenant-1", userId, UUID.randomUUID(), TargetType.THREAD));
        subscriptionRepository.save(Subscription.create("tenant-1", userId, UUID.randomUUID(), TargetType.THREAD));

        // When
        long count = subscriptionRepository.countByUserId(userId);

        // Then
        assertThat(count).isEqualTo(2);
    }
}
