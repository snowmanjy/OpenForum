package com.openforum.infra.jpa.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.openforum.infra.jpa.TestApplication;

@SpringBootTest(classes = { TestApplication.class })
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = { "forum-events-v1" })
class OutboxPublisherTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private com.openforum.infra.jpa.repository.MemberJpaRepository memberJpaRepository;

    @Test
    void shouldPublishAndDeleteOutboxEvents() {
        // Given
        UUID authorId = UUID.randomUUID();

        // Create and save a member first to satisfy FK constraint
        com.openforum.infra.jpa.entity.MemberEntity memberEntity = new com.openforum.infra.jpa.entity.MemberEntity(
                authorId,
                "ext-" + authorId,
                "test@example.com",
                "Test User",
                false,
                "tenant-test",
                java.time.Instant.now());
        memberJpaRepository.save(memberEntity);

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-test")
                .authorId(authorId)
                .title("Test Thread for Outbox")
                .isNew(true)
                .build();

        // When: Save thread (triggers outbox event creation)
        threadRepository.save(thread);

        // Then: Verify event is in Outbox
        List<OutboxEventEntity> events = outboxEventJpaRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo("ThreadCreatedEvent");

        // When: Wait for Scheduler (max 10s)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Then: Verify Outbox is empty (event deleted after publish)
            List<OutboxEventEntity> remainingEvents = outboxEventJpaRepository.findAll();
            assertThat(remainingEvents).isEmpty();
        });

        // Note: Verifying Kafka consumption requires a consumer, but empty outbox
        // implies
        // successful send because
        // OutboxEventPublisher.publishEvents() blocks on kafkaTemplate.send().get()
        // before deleting.
    }

    @Test
    void shouldProcessLargeBacklogInLoop() {
        // Given: 500 events in the outbox
        int totalEvents = 500;
        for (int i = 0; i < totalEvents; i++) {
            OutboxEventEntity event = new OutboxEventEntity();
            event.setId(UUID.randomUUID());
            event.setType("TestEvent");
            event.setPayload("{\"id\": " + i + "}");
            event.setCreatedAt(java.time.Instant.now());
            outboxEventJpaRepository.save(event);
        }

        assertThat(outboxEventJpaRepository.count()).isEqualTo(totalEvents);

        // When: Wait for Scheduler (max 15s to allow for multiple batches)
        // The loop should process 200, then 200, then 100 in quick succession without
        // waiting for the 5s delay between batches.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            // Then: Verify Outbox is empty
            long count = outboxEventJpaRepository.count();
            assertThat(count).isZero();
        });
    }
}
