package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.mapper.ThreadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ThreadRepositoryImplTest {

        @Mock
        private ThreadJpaRepository threadJpaRepository;
        @Mock
        private OutboxEventJpaRepository outboxEventJpaRepository;
        @Mock
        private JdbcTemplate jdbcTemplate;
        @Mock
        private ObjectMapper objectMapper;

        // Using real mapper for strict validation
        private ThreadMapper threadMapper = new ThreadMapper();

        private ThreadRepositoryImpl threadRepository;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                threadRepository = new ThreadRepositoryImpl(
                                threadJpaRepository,
                                outboxEventJpaRepository,
                                threadMapper,
                                objectMapper,
                                jdbcTemplate);
        }

        @Test
        void save_ExistingThread_ShouldPreserveDeletedStatus_AndUpdateTitle() {
                // Arrange
                UUID threadId = UUID.randomUUID();

                // Existing entity is DELETED
                ThreadEntity existingEntity = new ThreadEntity();
                existingEntity.setId(threadId);
                existingEntity.setTenantId("tenant-1");
                existingEntity.setAuthorId(UUID.randomUUID());
                existingEntity.setTitle("Old Title");
                existingEntity.setStatus(ThreadStatus.OPEN);
                existingEntity.setPostCount(5);
                existingEntity.setDeleted(true); // DELETED
                existingEntity.setDeletedAt(Instant.now());
                existingEntity.setCreatedAt(Instant.now().minusSeconds(3600));

                when(threadJpaRepository.findById(threadId)).thenReturn(Optional.of(existingEntity));

                // Domain object coming in with updated title (e.g. from an edit)
                // Domain object typically doesn't hold 'deleted' state if it was loaded cleanly
                // (or if we are just updating content).
                Thread updatedDomain = Thread.builder()
                                .id(threadId)
                                .tenantId("tenant-1")
                                .authorId(existingEntity.getAuthorId())
                                .title("Updated Title") // CHANGED
                                .status(ThreadStatus.OPEN)
                                .postCount(5)
                                .version(2L)
                                .deleted(true) // Domain object MUST reflect the deleted state if we want to preserve it
                                .deletedAt(existingEntity.getDeletedAt())
                                .build();

                // Act
                threadRepository.save(updatedDomain);

                // Assert
                ArgumentCaptor<ThreadEntity> entityCaptor = ArgumentCaptor.forClass(ThreadEntity.class);
                verify(threadJpaRepository).save(entityCaptor.capture());

                ThreadEntity savedEntity = entityCaptor.getValue();

                // 1. Verify title IS updated
                assertEquals("Updated Title", savedEntity.getTitle());

                // 2. CRITICAL: Verify deleted status is PRESERVED (still true)
                assertTrue(savedEntity.getDeleted(), "Deleted status must be preserved");
                assertEquals(existingEntity.getDeletedAt(), savedEntity.getDeletedAt());

                // 3. Verify other fields
                assertEquals(existingEntity.getAuthorId(), savedEntity.getAuthorId());
        }
}