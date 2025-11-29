package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PrivatePostCreatedEvent(
        UUID postId,
        UUID threadId,
        UUID authorId,
        String content,
        Instant createdAt) {
}
