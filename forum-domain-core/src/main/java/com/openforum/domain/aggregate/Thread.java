package com.openforum.domain.aggregate;

import com.openforum.domain.events.ThreadCreatedEvent;
import com.openforum.domain.factory.PostFactory;
import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import java.util.UUID;

public class Thread {
    private final UUID id;
    private final String tenantId;
    private final UUID authorId;
    private final UUID categoryId;
    private String title;
    private ThreadStatus status;
    private final Map<String, Object> metadata;
    private Long version;
    private final Instant createdAt;

    private final List<Object> domainEvents = new ArrayList<>();

    // TODO: Refactor to 'Write-Only Aggregate' if post count exceeds 1,000 for
    // performance.
    private final List<Post> posts = new ArrayList<>();

    private Thread(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.authorId = builder.authorId;
        this.categoryId = builder.categoryId;
        this.title = builder.title;
        this.status = builder.status;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.version = builder.version;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        if (builder.isNew) {
            this.domainEvents.add(new ThreadCreatedEvent(id, tenantId, authorId, title, this.createdAt));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String tenantId;
        private UUID authorId;
        private UUID categoryId;
        private String title;
        private ThreadStatus status = ThreadStatus.OPEN;
        private Map<String, Object> metadata;
        private Long version;
        private boolean isNew = false;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
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

        public Builder categoryId(UUID categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(ThreadStatus status) {
            this.status = status;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Thread build() {
            return new Thread(this);
        }
    }

    public Post addPost(String content, UUID authorId, boolean isBot) {
        // 1. Enforce Invariant (The DDD Win)
        if (this.status == ThreadStatus.CLOSED) {
            throw new IllegalStateException("Cannot add post to a closed thread.");
        }

        // 2. Create the Entity (Thread acts as Factory)
        Post newPost = PostFactory.create(
                this.tenantId,
                this.id,
                authorId,
                content,
                null, // replyToId
                isBot,
                java.util.List.of()); // mentionedUserIds

        // 3. Update Thread State (The Cohesion Win)
        // We don't need to load the List<Post> to update a counter or timestamp!
        // this.lastActivityAt = Instant.now();
        // this.postCount++;

        // 4. OPTIONAL: Do NOT add to 'this.posts' list if optimizing for scale.
        // Just return it for the repo to save.
        return newPost;
    }

    public List<Post> getPosts() {
        return List.copyOf(posts);
    }

    /**
     * Adds an imported post during bulk migration.
     * This bypasses normal invariant checks since we're reconstituting historical
     * data.
     * Should ONLY be called by ThreadFactory.createImported().
     * 
     * @param post Pre-existing post to add
     */
    public void addImportedPost(Post post) {
        this.posts.add(post);
    }

    public List<Object> pollEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public ThreadStatus getStatus() {
        return status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Changes the thread title if it's different from the current title.
     * Emits a ThreadTitleChanged event for history tracking.
     * 
     * @param newTitle The new title for the thread
     * @param byUserId The user making the change
     * @throws IllegalArgumentException if newTitle is null or empty
     */
    public void changeTitle(String newTitle, UUID byUserId) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Thread title cannot be null or empty");
        }

        // Only emit event if title actually changed
        if (!this.title.equals(newTitle)) {
            this.domainEvents.add(
                    new com.openforum.domain.events.ThreadTitleChanged(
                            this.id,
                            this.tenantId,
                            this.title,
                            newTitle,
                            byUserId,
                            Instant.now()));
            this.title = newTitle;
        }
    }

    /**
     * Closes the thread with a reason.
     * Emits a ThreadClosed event for history tracking.
     * 
     * @param reason   The reason for closing the thread
     * @param byUserId The user closing the thread
     * @throws IllegalStateException if thread is already closed
     */
    public void close(String reason, UUID byUserId) {
        if (this.status == ThreadStatus.CLOSED) {
            throw new IllegalStateException("Thread is already closed");
        }

        this.domainEvents.add(
                new com.openforum.domain.events.ThreadClosed(
                        this.id,
                        this.tenantId,
                        reason,
                        byUserId,
                        Instant.now()));
        this.status = ThreadStatus.CLOSED;
    }
}
