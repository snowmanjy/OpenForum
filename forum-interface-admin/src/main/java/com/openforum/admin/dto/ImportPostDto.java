package com.openforum.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for importing a single post in bulk migration.
 * All fields represent the state from the legacy system.
 */
public record ImportPostDto(
        @NotNull UUID id,
        @NotNull UUID authorId,
        @NotNull String content,
        UUID replyToPostId,
        Map<String, Object> metadata,
        boolean isBot,
        @NotNull LocalDateTime createdAt) {
}
