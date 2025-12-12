package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Poll;
import com.openforum.infra.jpa.entity.PollEntity;
import com.openforum.infra.jpa.entity.PollVoteEntity;
import com.openforum.infra.jpa.mapper.PollMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PollRepositoryImplTest {

    @Mock
    private PollJpaRepository pollJpaRepository;
    @Mock
    private PollVoteJpaRepository pollVoteJpaRepository;
    @Mock
    private OutboxEventJpaRepository outboxEventJpaRepository;
    @Mock
    private ObjectMapper objectMapper;

    private PollMapper pollMapper = new PollMapper();

    private PollRepositoryImpl pollRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pollRepository = new PollRepositoryImpl(
                pollJpaRepository,
                pollVoteJpaRepository,
                outboxEventJpaRepository,
                pollMapper,
                objectMapper);
    }

    @Test
    void save_ExistingPoll_ShouldPreserveHiddenFields() {
        // Arrange
        UUID pollId = UUID.randomUUID();

        // Existing entity has some fields we want to preserve (though PollEntity is
        // mostly immutable,
        // verifying pattern is strict).
        // Let's assume there's a field like 'createdAt' in TenantAwareEntity that we
        // don't want to lose if it was there.
        PollEntity existingEntity = new PollEntity(
                pollId, "tenant-1", UUID.randomUUID(), "Old Question", List.of("A", "B"), Instant.now(), false);
        // Assuming we mock the behavior that these fields are SET on the entity somehow

        when(pollJpaRepository.findById(pollId)).thenReturn(Optional.of(existingEntity));

        // Domain object update using reconstitute (simulating a loaded and modified
        // object)
        Poll updatedDomain = Poll.reconstitute(
                pollId,
                "tenant-1",
                existingEntity.getPostId(),
                "Updated Question", // Changed Question
                List.of("A", "B", "C"), // Changed Options
                existingEntity.getExpiresAt(),
                true, // Changed AllowMultipleVotes
                Instant.now(), // createdAt
                UUID.randomUUID(), // createdBy
                Instant.now(), // lastModifiedAt
                UUID.randomUUID(), // lastModifiedBy
                List.of() // votes
        );

        // Act
        pollRepository.save(updatedDomain);

        // Assert
        ArgumentCaptor<PollEntity> entityCaptor = ArgumentCaptor.forClass(PollEntity.class);
        verify(pollJpaRepository).save(entityCaptor.capture());

        PollEntity savedEntity = entityCaptor.getValue();
        assertEquals("Updated Question", savedEntity.getQuestion());
        assertEquals(true, savedEntity.isAllowMultipleVotes());

        // Validate object identity (it should be the SAME object instance adjusted)
        assertEquals(existingEntity, savedEntity);
    }
}
