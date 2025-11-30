package com.openforum.application.service;

import com.openforum.application.dto.CreatePrivatePostRequest;
import com.openforum.application.dto.CreatePrivateThreadRequest;
import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.domain.factory.PrivateThreadFactory;
import com.openforum.domain.repository.PrivateThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateThreadServiceTest {

    @Mock
    private PrivateThreadRepository privateThreadRepository;

    @Mock
    private PrivateThreadFactory privateThreadFactory;

    @InjectMocks
    private PrivateThreadService privateThreadService;

    @Test
    void createThread_shouldIncludeAuthorInParticipants() {
        // Given
        String tenantId = "default-tenant";
        UUID authorId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        CreatePrivateThreadRequest request = new CreatePrivateThreadRequest(
                List.of(otherUser),
                "Private Discussion",
                "Hello!");

        UUID threadId = UUID.randomUUID();
        PrivateThread thread = mock(PrivateThread.class);
        when(thread.getId()).thenReturn(threadId);
        when(privateThreadFactory.create(anyString(), anyList(), anyString())).thenReturn(thread);

        // When
        UUID result = privateThreadService.createThread(tenantId, authorId, request);

        // Then
        assertThat(result).isEqualTo(threadId);
        verify(privateThreadFactory).create(eq(tenantId),
                argThat(participants -> participants.contains(authorId) && participants.contains(otherUser)),
                eq("Private Discussion"));
        verify(thread).addPost("Hello!", authorId);
        verify(privateThreadRepository).save(thread);
    }

    @Test
    void createThread_shouldNotDuplicateAuthor_whenAlreadyInParticipants() {
        // Given
        String tenantId = "default-tenant";
        UUID authorId = UUID.randomUUID();
        CreatePrivateThreadRequest request = new CreatePrivateThreadRequest(
                List.of(authorId), // Author already in list
                "Solo Thread",
                null);

        UUID threadId = UUID.randomUUID();
        PrivateThread thread = mock(PrivateThread.class);
        when(thread.getId()).thenReturn(threadId);
        when(privateThreadFactory.create(anyString(), anyList(), anyString())).thenReturn(thread);

        // When
        privateThreadService.createThread(tenantId, authorId, request);

        // Then
        verify(privateThreadFactory).create(eq(tenantId),
                argThat(participants -> participants.size() == 1 && participants.contains(authorId)),
                eq("Solo Thread"));
    }

    @Test
    void addPost_shouldSucceed_whenUserIsParticipant() {
        // Given
        String tenantId = "default-tenant";
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        CreatePrivatePostRequest request = new CreatePrivatePostRequest("New message");

        PrivateThread thread = mock(PrivateThread.class);
        when(privateThreadRepository.findByIdAndParticipantId(threadId, authorId))
                .thenReturn(Optional.of(thread));

        // When
        privateThreadService.addPost(tenantId, threadId, authorId, request);

        // Then
        verify(thread).addPost("New message", authorId);
        verify(privateThreadRepository).save(thread);
    }

    @Test
    void addPost_shouldFail_whenUserIsNotParticipant() {
        // Given
        String tenantId = "default-tenant";
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        CreatePrivatePostRequest request = new CreatePrivatePostRequest("Unauthorized message");

        when(privateThreadRepository.findByIdAndParticipantId(threadId, authorId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> privateThreadService.addPost(tenantId, threadId, authorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thread not found or user is not a participant");
    }
}
