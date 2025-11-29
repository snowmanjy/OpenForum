package com.openforum.admin.service;

import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.BulkImportResponse;
import com.openforum.admin.dto.ImportPostDto;
import com.openforum.admin.dto.ImportThreadDto;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock
    private ThreadRepository threadRepository;

    private BulkImportService bulkImportService;

    @BeforeEach
    void setUp() {
        bulkImportService = new BulkImportService(threadRepository);
    }

    @Test
    void shouldImportThreadsAndPostsSuccessfully() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant now = Instant.now();

        ImportPostDto postDto = new ImportPostDto(
                postId,
                authorId,
                "Test Content",
                null,
                Map.of(),
                false,
                now);

        ImportThreadDto threadDto = new ImportThreadDto(
                threadId,
                "tenant-1",
                authorId,
                "Test Thread",
                ThreadStatus.OPEN,
                now,
                Map.of(),
                List.of(postDto));

        BulkImportRequest request = new BulkImportRequest(List.of(threadDto));

        // When
        BulkImportResponse response = bulkImportService.importThreads(request);

        // Then
        assertThat(response.threadsImported()).isEqualTo(1);
        assertThat(response.postsImported()).isEqualTo(1);
        assertThat(response.errors()).isEmpty();

        ArgumentCaptor<List<Thread>> captor = ArgumentCaptor.forClass(List.class);
        verify(threadRepository).saveAll(captor.capture());

        List<Thread> savedThreads = captor.getValue();
        assertThat(savedThreads).hasSize(1);

        Thread savedThread = savedThreads.get(0);
        assertThat(savedThread.getId()).isEqualTo(threadId);
        assertThat(savedThread.getPosts()).hasSize(1);
        assertThat(savedThread.getPosts().get(0).getId()).isEqualTo(postId);

        // Critical: Verify no events generated
        assertThat(savedThread.pollEvents()).isEmpty();
    }
}
