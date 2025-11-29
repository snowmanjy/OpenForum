package com.openforum.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivatePostDto(
        UUID id,
        UUID authorId,
        String content,
        Instant createdAt) {
}
