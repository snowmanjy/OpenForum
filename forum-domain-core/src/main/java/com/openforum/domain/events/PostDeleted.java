package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PostDeleted(
        UUID postId,
        UUID threadId,
        String tenantId,
        String reason,
        UUID byUserId,
        Instant deletedAt) implements DomainEvent {
}
