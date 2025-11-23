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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase // Defaults to H2
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({ ThreadRepositoryImpl.class, com.openforum.infra.jpa.mapper.ThreadMapper.class, JpaTestConfig.class })
class ThreadRepositoryImplTest {

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void should_save_thread_and_events_atomically() {
        // Given
        Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), "Integration Test", Map.of("key", "value"));
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
}
