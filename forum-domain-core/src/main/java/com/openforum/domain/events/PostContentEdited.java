package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PostContentEdited(
        UUID postId,
        UUID threadId,
        String tenantId,
        String oldContent,
        String newContent,
        UUID byUserId,
        Instant editedAt) implements DomainEvent {
}
