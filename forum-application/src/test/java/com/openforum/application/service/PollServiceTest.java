package com.openforum.application.service;

import com.openforum.application.dto.CreatePollRequest;
import com.openforum.application.dto.PollDto;
import com.openforum.application.dto.VotePollRequest;
import com.openforum.domain.aggregate.Poll;
import com.openforum.domain.factory.PollFactory;
import com.openforum.domain.repository.PollRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollFactory pollFactory;

    @InjectMocks
    private PollService pollService;

    @Test
    void createPoll_shouldSucceed() {
        // Given
        String tenantId = "default-tenant";
        UUID postId = UUID.randomUUID();
        CreatePollRequest request = new CreatePollRequest(
                "What is your favorite color?",
                List.of("Red", "Blue", "Green"),
                Instant.now().plusSeconds(86400),
                false);

        UUID pollId = UUID.randomUUID();
        Poll poll = mock(Poll.class);
        when(poll.getId()).thenReturn(pollId);
        when(pollFactory.create(anyString(), any(UUID.class), anyString(), anyList(), any(), anyBoolean()))
                .thenReturn(poll);

        // When
        UUID result = pollService.createPoll(tenantId, postId, request);

        // Then
        assertThat(result).isEqualTo(pollId);
        verify(pollRepository).save(poll);
    }

    @Test
    void castVote_shouldSucceed_whenPollExists() {
        // Given
        String tenantId = "default-tenant";
        UUID pollId = UUID.randomUUID();
        UUID voterId = UUID.randomUUID();
        VotePollRequest request = new VotePollRequest(1);

        Poll poll = mock(Poll.class);
        when(poll.getTenantId()).thenReturn(tenantId);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        // When
        pollService.castVote(tenantId, pollId, voterId, request);

        // Then
        verify(poll).castVote(voterId, 1);
        verify(pollRepository).save(poll);
    }

    @Test
    void castVote_shouldFail_whenPollNotFound() {
        // Given
        String tenantId = "default-tenant";
        UUID pollId = UUID.randomUUID();
        UUID voterId = UUID.randomUUID();
        VotePollRequest request = new VotePollRequest(1);

        when(pollRepository.findById(pollId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pollService.castVote(tenantId, pollId, voterId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll not found");
    }

    @Test
    void castVote_shouldFail_whenTenantMismatch() {
        // Given
        String tenantId = "tenant-1";
        UUID pollId = UUID.randomUUID();
        UUID voterId = UUID.randomUUID();
        VotePollRequest request = new VotePollRequest(1);

        Poll poll = mock(Poll.class);
        when(poll.getTenantId()).thenReturn("tenant-2");
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        // When & Then
        assertThatThrownBy(() -> pollService.castVote(tenantId, pollId, voterId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poll not found in tenant");
    }
}
