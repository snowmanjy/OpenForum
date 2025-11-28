package com.openforum.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SubscriptionWithThreadDto {
    private final UUID threadId;
    private final String threadTitle;
    private final LocalDateTime subscribedAt;

    public SubscriptionWithThreadDto(UUID threadId, String threadTitle, LocalDateTime subscribedAt) {
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

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }
}
