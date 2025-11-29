package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ThreadCreatedEvent(
        UUID threadId,
        String tenantId,
        UUID authorId,
        String title,
        Instant createdAt) implements DomainEvent {
}
