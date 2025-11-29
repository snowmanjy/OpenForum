package com.openforum.domain.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrivateThreadCreatedEvent(
        UUID threadId,
        String tenantId,
        List<UUID> participantIds,
        String subject,
        Instant createdAt) {
}
