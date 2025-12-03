package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ThreadTitleChanged(
        UUID threadId,
        String tenantId,
        String oldTitle,
        String newTitle,
        UUID byUserId,
        Instant changedAt) implements DomainEvent {
}
