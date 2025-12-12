package com.openforum.domain.aggregate;

import com.openforum.domain.events.PostCreatedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Post {
    private final UUID id;
    private final UUID threadId;
    private final String tenantId;
    private final UUID authorId;
    private String content; // Made mutable for editing
    private final Long version;
    private final UUID replyToPostId;
    private final Map<String, Object> metadata;
    private final Instant createdAt;
    private final List<UUID> mentionedMemberIds;
    private Integer postNumber;
    private boolean isDeleted = false;
    private int score = 0;

    private final List<Double> embedding;
    private Instant deletedAt;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;
    private final UUID createdBy;

    private final List<Object> domainEvents = new ArrayList<>();

    private Post(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.threadId = Objects.requireNonNull(builder.threadId, "threadId must not be null");
        this.tenantId = Objects.requireNonNull(builder.tenantId, "tenantId must not be null");
        this.authorId = Objects.requireNonNull(builder.authorId, "authorId must not be null");
        this.content = Objects.requireNonNull(builder.content, "content must not be null");
        this.version = builder.version;
        this.replyToPostId = builder.replyToPostId;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.mentionedMemberIds = builder.mentionedMemberIds != null ? List.copyOf(builder.mentionedMemberIds)
                : List.of();
        this.postNumber = builder.postNumber;
        this.isDeleted = builder.isDeleted;
        this.score = builder.score;
        this.embedding = builder.embedding != null ? List.copyOf(builder.embedding) : null;
        this.deletedAt = builder.deletedAt;
        this.lastModifiedAt = builder.lastModifiedAt;
        this.lastModifiedBy = builder.lastModifiedBy;
        this.createdBy = builder.createdBy;

        if (builder.isNew) {
            this.domainEvents.add(new PostCreatedEvent(id, threadId, authorId, content, createdAt, builder.isBot,
                    mentionedMemberIds));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID threadId;
        private String tenantId;
        private UUID authorId;
        private String content;
        private Long version;
        private UUID replyToPostId;
        private Map<String, Object> metadata;
        private Instant createdAt;
        private List<UUID> mentionedMemberIds;
        private Integer postNumber;
        private boolean isNew = false;
        private boolean isBot = false;
        private boolean isDeleted = false;
        private int score = 0;
        private List<Double> embedding;
        private Instant deletedAt;
        private Instant lastModifiedAt;
        private UUID lastModifiedBy;
        private UUID createdBy;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder threadId(UUID threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder authorId(UUID authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Builder replyToPostId(UUID replyToPostId) {
            this.replyToPostId = replyToPostId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder mentionedMemberIds(List<UUID> mentionedMemberIds) {
            this.mentionedMemberIds = mentionedMemberIds;
            return this;
        }

        public Builder postNumber(Integer postNumber) {
            this.postNumber = postNumber;
            return this;
        }

        public Builder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder isBot(boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        public Builder isDeleted(boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Builder score(int score) {
            this.score = score;
            return this;
        }

        public Builder embedding(List<Double> embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder deletedAt(Instant deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Builder lastModifiedAt(Instant lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder lastModifiedBy(UUID lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Post build() {
            return new Post(this);
        }
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

    public Long getVersion() {
        return version;
    }

    public UUID getReplyToPostId() {
        return replyToPostId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<UUID> getMentionedMemberIds() {
        return mentionedMemberIds;
    }

    public Integer getPostNumber() {
        return postNumber;
    }

    public int getScore() {
        return score;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public List<Object> pollEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Edits the post content if it's different from the current content.
     * Emits a PostContentEdited event for history tracking.
     * 
     * @param newContent The new content for the post
     * @param byMemberId The user making the edit
     * @throws IllegalArgumentException if newContent is null or empty
     * @throws IllegalStateException    if post is deleted
     */
    public void editContent(String newContent, UUID byMemberId) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Post content cannot be null or empty");
        }

        if (this.isDeleted) {
            throw new IllegalStateException("Cannot edit a deleted post");
        }

        // Only emit event if content actually changed
        if (!this.content.equals(newContent)) {
            this.domainEvents.add(
                    new com.openforum.domain.events.PostContentEdited(
                            this.id,
                            this.threadId,
                            this.tenantId,
                            this.content,
                            newContent,
                            byMemberId,
                            Instant.now()));
            this.content = newContent;
        }
    }

    /**
     * Marks the post as deleted.
     * Emits a PostDeleted event for history tracking.
     * 
     * @param reason     The reason for deletion
     * @param byMemberId The user deleting the post
     * @throws IllegalStateException if post is already deleted
     */
    public void delete(String reason, UUID byMemberId) {
        if (this.isDeleted) {
            throw new IllegalStateException("Post is already deleted");
        }

        Instant now = Instant.now();

        this.domainEvents.add(
                new com.openforum.domain.events.PostDeleted(
                        this.id,
                        this.threadId,
                        this.tenantId,
                        reason,
                        byMemberId,
                        now));
        this.isDeleted = true;
        this.deletedAt = now;
    }
}
