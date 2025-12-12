package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PostContentEdited(
        UUID postId,
        UUID threadId,
        String tenantId,
        String oldContent,
        String newContent,
        UUID byMemberId,
        Instant editedAt) implements DomainEvent {
}
