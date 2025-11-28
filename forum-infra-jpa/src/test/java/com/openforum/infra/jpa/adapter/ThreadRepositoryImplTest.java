package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadFactory;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers // <--- 1. Enable Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // <--- 2. Disable H2
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
})
@Import({ ThreadRepositoryImpl.class, com.openforum.infra.jpa.mapper.ThreadMapper.class, JpaTestConfig.class })
class ThreadRepositoryImplTest {

        // 3. Define the Real Postgres Container
        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Autowired
        private ThreadRepository threadRepository;

        @Autowired
        private OutboxEventJpaRepository outboxEventJpaRepository;

        @Test
        void should_save_thread_and_events_atomically() {
                // Given
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Integration Test",
                                Map.of("key", "value"));
                UUID id = thread.getId();

                // When
                threadRepository.save(thread);

                // Then
                Optional<Thread> savedThread = threadRepository.findById(id);
                assertThat(savedThread).isPresent();
                assertThat(savedThread.get().getTitle()).isEqualTo("Integration Test");
                assertThat(savedThread.get().getMetadata()).containsEntry("key", "value");
                assertThat(savedThread.get().getVersion()).isNotNull();

                List<OutboxEventEntity> events = outboxEventJpaRepository.findAll();
                assertThat(events).hasSize(1);
                assertThat(events.get(0).getType()).isEqualTo("ThreadCreatedEvent");
                assertThat(events.get(0).getPayload()).contains("Integration Test");
        }

        @Test
        void shouldSaveAllThreadsWithImportEvents() {
                // Given
                Thread thread1 = ThreadFactory.createImported(
                                UUID.randomUUID(), "tenant-import", UUID.randomUUID(), null, "Import 1",
                                com.openforum.domain.aggregate.ThreadStatus.OPEN, java.time.LocalDateTime.now(),
                                Map.of(), List.of());

                Thread thread2 = ThreadFactory.createImported(
                                UUID.randomUUID(), "tenant-import", UUID.randomUUID(), null, "Import 2",
                                com.openforum.domain.aggregate.ThreadStatus.CLOSED, java.time.LocalDateTime.now(),
                                Map.of(), List.of());

                int initialEventCount = outboxEventJpaRepository.findAll().size();

                // When
                threadRepository.saveAll(List.of(thread1, thread2));

                // Then
                assertThat(threadRepository.findById(thread1.getId())).isPresent();
                assertThat(threadRepository.findById(thread2.getId())).isPresent();

                List<OutboxEventEntity> currentEvents = outboxEventJpaRepository.findAll();
                // Expect: Initial + 2 Import Events
                assertThat(currentEvents).hasSizeGreaterThanOrEqualTo(initialEventCount + 2);

                boolean hasImportEvent = currentEvents.stream()
                                .anyMatch(e -> e.getType().equals("ThreadImportedEvent"));
                assertThat(hasImportEvent).isTrue();
        }

        @Test
        void shouldGenerateEventsForImportedPosts() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();

                ThreadFactory.ImportedPostData postData = new ThreadFactory.ImportedPostData(
                                UUID.randomUUID(), authorId, "Post Content", null, Map.of(), false,
                                java.time.LocalDateTime.now());

                Thread thread = ThreadFactory.createImported(
                                threadId, "tenant-import", authorId, null, "Thread with Post",
                                com.openforum.domain.aggregate.ThreadStatus.OPEN, java.time.LocalDateTime.now(),
                                Map.of(), List.of(postData));

                // When
                threadRepository.saveAll(List.of(thread));

                // Then
                List<OutboxEventEntity> events = outboxEventJpaRepository.findAll();
                boolean hasPostEvent = events.stream()
                                .anyMatch(e -> e.getType().equals("PostImportedEvent"));

                assertThat(hasPostEvent).isTrue();
        }
}