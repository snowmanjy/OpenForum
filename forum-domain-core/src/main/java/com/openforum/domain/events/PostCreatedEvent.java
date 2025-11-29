package com.openforum.domain.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostCreatedEvent(
        UUID postId,
        UUID threadId,
        UUID authorId,
        String content,
        Instant createdAt,
        boolean isBot,
        List<UUID> mentionedUserIds) implements DomainEvent {
}
