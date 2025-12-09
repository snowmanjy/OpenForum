package com.openforum.application.service;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadServiceTest {

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private com.openforum.domain.repository.PostRepository postRepository;

    @InjectMocks
    private ThreadService threadService;

    @Test
    void createThread_shouldPersistAndReturnThread() {
        // Given
        String tenantId = "tenant-1";
        UUID authorId = UUID.randomUUID();
        String title = "Test Thread";
        String content = "Test Content";

        // When
        Thread result = threadService.createThread(tenantId, authorId, title, content);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getId()).isNotNull();

        verify(threadRepository).save(any(Thread.class));
        verify(postRepository).save(any(com.openforum.domain.aggregate.Post.class));
    }

    @Test
    void createThread_shouldPropagateRepositoryException() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(threadRepository).save(any(Thread.class));

        // When/Then
        assertThatThrownBy(() -> threadService.createThread("t1", UUID.randomUUID(), "Title", "Content"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    @Test
    void getThread_shouldReturnThread_whenExists() {
        // Given
        UUID id = UUID.randomUUID();
        Thread thread = mock(Thread.class);
        when(threadRepository.findById(id)).thenReturn(Optional.of(thread));

        // When
        Optional<Thread> result = threadService.getThread(id);

        // Then
        assertThat(result).isPresent().contains(thread);
    }

    @Test
    void getThread_shouldReturnEmpty_whenNotExists() {
        // Given
        UUID id = UUID.randomUUID();
        when(threadRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<Thread> result = threadService.getThread(id);

        // Then
        assertThat(result).isEmpty();
    }
}
