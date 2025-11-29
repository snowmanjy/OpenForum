package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PostImportedEvent(
                UUID postId,
                UUID threadId,
                UUID authorId,
                String content,
                boolean isBot,
                Instant createdAt) implements DomainEvent {
}
