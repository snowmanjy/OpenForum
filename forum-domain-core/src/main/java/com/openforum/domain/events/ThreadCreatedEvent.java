package com.openforum.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThreadCreatedEvent(
                UUID threadId,
                String tenantId,
                UUID authorId,
                String title,
                LocalDateTime occurredOn) implements DomainEvent {
}
