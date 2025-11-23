package com.openforum.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for bulk import endpoint.
 * Contains a list of threads with their posts to import.
 */
public record BulkImportRequest(
        @NotEmpty(message = "Threads list cannot be empty")
        @Valid List<ImportThreadDto> threads) {
}
