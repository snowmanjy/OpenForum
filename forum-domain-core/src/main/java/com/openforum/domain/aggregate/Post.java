package com.openforum.domain.aggregate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Post {
    private final UUID id;
    private final UUID threadId;
    private final UUID authorId;
    private final String content;
    private final Long version;
    private final UUID replyToPostId;
    private final Map<String, Object> metadata;

    private Post(Builder builder) {
            
        this.id = Objects.requireNonNull(builder.id, "id must not be null");
        this.threadId = Objects.requireNonNull(builder.threadId, "threadId must not be null");
        this.authorId = Objects.requireNonNull(builder.authorId, "authorId must not be null");
        this.content = Objects.requireNonNull(builder.content, "content must not be null");
        this.version = builder.version;
        this.replyToPostId = builder.replyToPostId;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID threadId;
        private UUID authorId;
        private String content;
        private Long version;
        private UUID replyToPostId;
        private Map<String, Object> metadata;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder threadId(UUID threadId) {
            this.threadId = threadId;
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
}
