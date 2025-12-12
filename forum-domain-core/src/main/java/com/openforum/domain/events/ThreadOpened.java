package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a thread is opened/reopened.
 */
public record ThreadOpened(
        UUID threadId,
        String tenantId,
        String reason,
        UUID openedBy,
        Instant openedAt) implements DomainEvent {
}
