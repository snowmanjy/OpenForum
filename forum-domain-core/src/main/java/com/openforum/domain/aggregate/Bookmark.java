package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bookmark aggregate - represents a user saving a post to their private
 * collection.
 * Distinct from upvotes and subscriptions - no notifications are triggered.
 */
public class Bookmark {

    private final UUID id;
    private final String tenantId;
    private final UUID memberId;
    private final UUID postId;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;

    private Bookmark(UUID id, String tenantId, UUID memberId, UUID postId, Instant createdAt,
            UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.postId = Objects.requireNonNull(postId, "postId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = createdBy;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Factory method to create a new bookmark.
     */
    public static Bookmark create(String tenantId, UUID memberId, UUID postId) {
        Instant now = Instant.now();
        return new Bookmark(
                UUID.randomUUID(),
                tenantId,
                memberId,
                postId,
                now,
                memberId, // createdBy
                now, // lastModifiedAt
                memberId // lastModifiedBy
        );
    }

    /**
     * Reconstitute a bookmark from persistence.
     */
    public static Bookmark reconstitute(UUID id, String tenantId, UUID memberId, UUID postId, Instant createdAt,
            UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        return new Bookmark(id, tenantId, memberId, postId, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getPostId() {
        return postId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Bookmark bookmark = (Bookmark) o;
        return Objects.equals(id, bookmark.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
