package com.openforum.admin.dto;

import com.openforum.domain.aggregate.ThreadStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for importing a single thread with its posts in bulk migration.
 * All fields represent the state from the legacy system.
 */
public record ImportThreadDto(
        @NotNull UUID id,
        @NotBlank String tenantId,
        @NotNull UUID authorId,
        @NotBlank String title,
        ThreadStatus status,
        @NotNull LocalDateTime createdAt,
        Map<String, Object> metadata,
        @Valid @NotNull List<ImportPostDto> posts) {
}
