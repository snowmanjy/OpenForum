package com.openforum.application.service;

import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ThreadArchivalServiceTest {

    private ThreadRepository threadRepository;
    private ThreadArchivalService threadArchivalService;

    @BeforeEach
    void setUp() {
        threadRepository = mock(ThreadRepository.class);
        threadArchivalService = new ThreadArchivalService(threadRepository, 365);
    }

    @Test
    void archiveStaleThreads_ShouldCallRepositoryWithCorrectCutoff() {
        // Given
        when(threadRepository.archiveStaleThreads(any())).thenReturn(10);

        // When
        threadArchivalService.archiveStaleThreads();

        // Then
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(threadRepository).archiveStaleThreads(captor.capture());

        Instant cutoff = captor.getValue();
        Instant expected = Instant.now().minus(365, ChronoUnit.DAYS);

        // Allow slight difference for execution time
        long diff = Math.abs(ChronoUnit.SECONDS.between(expected, cutoff));
        assertTrue(diff < 5, "Cutoff time should be within 5 seconds of expected");
    }
}
