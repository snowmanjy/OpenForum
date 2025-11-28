package com.openforum.infra.jpa.adapter;

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
}
