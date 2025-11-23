package com.openforum.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostImportedEvent(
        UUID postId,
        UUID threadId,
        UUID authorId,
        String content,
        boolean isBot,
        LocalDateTime createdAt) {
}
