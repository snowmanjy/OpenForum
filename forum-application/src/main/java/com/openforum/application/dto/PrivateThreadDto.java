package com.openforum.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrivateThreadDto(
        UUID id,
        String subject,
        List<UUID> participantIds,
        Instant createdAt,
        Instant lastActivityAt,
        List<PrivatePostDto> posts) {
}
