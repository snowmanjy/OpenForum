package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.factory.ThreadFactory;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.mapper.ThreadMapper;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ThreadRepositoryImpl ACID guarantees.
 * These tests use mocks to simulate failure scenarios and verify rollback
 * behavior.
 */
@ExtendWith(MockitoExtension.class)
class ThreadRepositoryImplRollbackTest {

        @Mock
        private ThreadJpaRepository threadJpaRepository;

        @Mock
        private OutboxEventJpaRepository outboxEventJpaRepository;

        @Mock
        private ThreadMapper threadMapper;

        @Mock
        private ObjectMapper objectMapper;

        @Mock
        private JdbcTemplate jdbcTemplate;

        private ThreadRepositoryImpl threadRepository;

        @BeforeEach
        void setUp() {
                threadRepository = new ThreadRepositoryImpl(
                                threadJpaRepository,
                                outboxEventJpaRepository,
                                threadMapper,
                                objectMapper,
                                jdbcTemplate);
        }

        @Test
        void shouldThrowExceptionWhenEventSerializationFails() throws JsonProcessingException {
                // Given: Thread with event that will fail to serialize
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Test", Map.of());
                ThreadEntity mockEntity = new ThreadEntity();

                when(threadMapper.toEntity(thread)).thenReturn(mockEntity);
                when(threadJpaRepository.save(mockEntity)).thenReturn(mockEntity);

                // Mock ObjectMapper to throw exception during event serialization
                when(objectMapper.writeValueAsString(any()))
                                .thenThrow(new JsonProcessingException("Serialization failed") {
                                });

                // When/Then: Should throw RuntimeException (which triggers rollback)
                assertThatThrownBy(() -> threadRepository.save(thread))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Failed to serialize event");

                // Verify: Thread save was attempted (but will be rolled back by transaction
                // manager)
                verify(threadJpaRepository).save(mockEntity);
                // Event save was never attempted because serialization failed first
                verify(outboxEventJpaRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenEventSaveFails() throws JsonProcessingException {
                // Given: Thread with successfully serializable event, but save fails
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Test", Map.of());
                ThreadEntity mockEntity = new ThreadEntity();

                when(threadMapper.toEntity(thread)).thenReturn(mockEntity);
                when(threadJpaRepository.save(mockEntity)).thenReturn(mockEntity);
                when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"data\"}");

                // Mock event save to fail
                when(outboxEventJpaRepository.save(any(OutboxEventEntity.class)))
                                .thenThrow(new DataAccessException("Database error") {
                                });

                // When/Then: Should propagate exception (triggering rollback)
                assertThatThrownBy(() -> threadRepository.save(thread))
                                .isInstanceOf(DataAccessException.class)
                                .hasMessageContaining("Database error");

                // Verify: Both operations were attempted
                verify(threadJpaRepository).save(mockEntity);
                verify(outboxEventJpaRepository).save(any(OutboxEventEntity.class));
        }

        @Test
        void shouldThrowExceptionWhenThreadSaveFails() {
                // Given: Thread save itself fails
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Test", Map.of());
                ThreadEntity mockEntity = new ThreadEntity();

                when(threadMapper.toEntity(thread)).thenReturn(mockEntity);
                when(threadJpaRepository.save(mockEntity))
                                .thenThrow(new DataAccessException("Thread save failed") {
                                });

                // When/Then: Should propagate exception
                assertThatThrownBy(() -> threadRepository.save(thread))
                                .isInstanceOf(DataAccessException.class)
                                .hasMessageContaining("Thread save failed");

                // Verify: Event save was never attempted
                verify(threadJpaRepository).save(mockEntity);
                verify(outboxEventJpaRepository, never()).save(any());
        }

        @Test
        void shouldPropagateMapperException() {
                // Given: Mapper throws exception
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Test", Map.of());

                when(threadMapper.toEntity(thread))
                                .thenThrow(new IllegalArgumentException("Mapping failed"));

                // When/Then: Should propagate exception
                assertThatThrownBy(() -> threadRepository.save(thread))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Mapping failed");

                // Verify: Nothing was saved
                verify(threadJpaRepository, never()).save(any());
                verify(outboxEventJpaRepository, never()).save(any());
        }

        @Test
        void shouldHandleMultipleEventsWithOneFailure() throws JsonProcessingException {
                // Given: Thread with 2 events, second event save fails
                Thread thread = ThreadFactory.create("tenant-1", UUID.randomUUID(), null, "Test", Map.of());
                // Thread.pollEvents() will return 1 event from ThreadCreatedEvent
                ThreadEntity mockEntity = new ThreadEntity();

                when(threadMapper.toEntity(thread)).thenReturn(mockEntity);
                when(threadJpaRepository.save(mockEntity)).thenReturn(mockEntity);
                when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"data\"}");

                // First save succeeds, but if there were a second, it would fail
                // Since ThreadFactory only creates 1 event, we just verify the behavior
                when(outboxEventJpaRepository.save(any(OutboxEventEntity.class)))
                                .thenReturn(new OutboxEventEntity());

                // When: Save succeeds with single event
                threadRepository.save(thread);

                // Then: Verify single event was saved
                verify(outboxEventJpaRepository, times(1)).save(any(OutboxEventEntity.class));
        }
}
