package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record ThreadClosed(
        UUID threadId,
        String tenantId,
        String reason,
        UUID byMemberId,
        Instant closedAt) implements DomainEvent {
}
