package com.openforum.infra.jpa.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection interface for fetching Thread with OP content in a single query.
 * Used to prevent N+1 fetches on the thread list API.
 */
public interface ThreadWithOPProjection {
    UUID getId();

    String getTitle();

    String getStatus();

    String getContent(); // OP content from posts table

    Instant getCreatedAt();

    UUID getAuthorId();

    Integer getPostCount();
}
