package com.openforum.application.dto;

import java.time.Instant;
import java.util.UUID;

public class SubscriptionWithThreadDto {
    private final UUID threadId;
    private final String threadTitle;
    private final Instant subscribedAt;

    public SubscriptionWithThreadDto(UUID threadId, String threadTitle, Instant subscribedAt) {
        this.threadId = threadId;
        this.threadTitle = threadTitle;
        this.subscribedAt = subscribedAt;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public String getThreadTitle() {
        return threadTitle;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }
}
