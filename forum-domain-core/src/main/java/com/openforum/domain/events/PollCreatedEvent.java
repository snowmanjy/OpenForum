package com.openforum.domain.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PollCreatedEvent(
        UUID pollId,
        String tenantId,
        UUID postId,
        String question,
        List<String> options,
        Instant expiresAt,
        boolean allowMultipleVotes,
        Instant createdAt) {
}
