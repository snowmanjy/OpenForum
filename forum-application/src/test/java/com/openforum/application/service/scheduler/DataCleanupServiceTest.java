package com.openforum.application.service.scheduler;

import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ThreadRepository threadRepository;

    @InjectMocks
    private DataCleanupService dataCleanupService;

    @Test
    void cleanupOldData_shouldDeleteInBatches() {
        // Given
        ReflectionTestUtils.setField(dataCleanupService, "retentionDays", 30);

        // Mock post deletion: 2 batches (1000 + 500) then 0
        when(postRepository.deleteBatch(any(Instant.class), anyInt()))
                .thenReturn(1000)
                .thenReturn(500)
                .thenReturn(0);

        // Mock thread deletion: 1 batch (200) then 0
        when(threadRepository.deleteBatch(any(Instant.class), anyInt()))
                .thenReturn(200)
                .thenReturn(0);

        // When
        dataCleanupService.cleanupOldData();

        // Then
        verify(postRepository, times(3)).deleteBatch(any(Instant.class), eq(1000));
        verify(threadRepository, times(2)).deleteBatch(any(Instant.class), eq(1000));
    }
}
