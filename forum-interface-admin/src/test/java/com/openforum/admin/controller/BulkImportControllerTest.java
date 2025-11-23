package com.openforum.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.BulkImportResponse;
import com.openforum.admin.dto.ImportPostDto;
import com.openforum.admin.dto.ImportThreadDto;
import com.openforum.admin.service.BulkImportService;
import com.openforum.domain.aggregate.ThreadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.openforum.admin.TestConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BulkImportController.class)
@Import(TestConfig.class)
class BulkImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BulkImportService bulkImportService;

    @Test
    void shouldImportThreadsSuccessfully() throws Exception {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ImportPostDto postDto = new ImportPostDto(
                UUID.randomUUID(),
                authorId,
                "Test Content",
                null,
                Map.of(),
                false,
                now
        );

        ImportThreadDto threadDto = new ImportThreadDto(
                threadId,
                "tenant-1",
                authorId,
                "Test Thread",
                ThreadStatus.OPEN,
                now,
                Map.of(),
                List.of(postDto)
        );

        BulkImportRequest request = new BulkImportRequest(List.of(threadDto));
        BulkImportResponse response = BulkImportResponse.success(1, 1);

        when(bulkImportService.importThreads(any(BulkImportRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/admin/v1/bulk/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threadsImported").value(1))
                .andExpect(jsonPath("$.postsImported").value(1));
    }

    @Test
    void shouldReturnBadRequestForInvalidInput() throws Exception {
        // Given - Empty request
        BulkImportRequest request = new BulkImportRequest(List.of());

        // When/Then
        mockMvc.perform(post("/admin/v1/bulk/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
