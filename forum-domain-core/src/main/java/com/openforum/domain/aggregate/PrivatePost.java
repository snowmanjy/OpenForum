package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a post within a private thread (DM).
 * Physically isolated from public posts.
 */
public class PrivatePost {
    private final UUID id;
    private final UUID threadId;
    private final UUID authorId;
    private final String content;
    private final Instant createdAt;

    public PrivatePost(UUID id, UUID threadId, UUID authorId, String content, Instant createdAt) {
        this.id = id;
        this.threadId = threadId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static PrivatePost create(UUID threadId, UUID authorId, String content) {
        return new PrivatePost(
                UUID.randomUUID(),
                threadId,
                authorId,
                content,
                Instant.now());
    }

    // Reconstitute from persistence
    public static PrivatePost reconstitute(UUID id, UUID threadId, UUID authorId, String content, Instant createdAt) {
        return new PrivatePost(id, threadId, authorId, content, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
