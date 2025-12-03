package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ThreadClosed(
        UUID threadId,
        String tenantId,
        String reason,
        UUID byUserId,
        Instant closedAt) implements DomainEvent {
}
