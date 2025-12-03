package com.openforum.infra.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    private UUID id;

    private UUID threadId;
    private String tenantId;
    private UUID authorId;
    private String content;
    private Long version;
    private UUID replyToPostId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "post_mentions", joinColumns = @jakarta.persistence.JoinColumn(name = "post_id"))
    @jakarta.persistence.Column(name = "user_id")
    private java.util.List<UUID> mentionedUserIds = new java.util.ArrayList<>();

    private Instant createdAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UUID getReplyToPostId() {
        return replyToPostId;
    }

    public void setReplyToPostId(UUID replyToPostId) {
        this.replyToPostId = replyToPostId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public java.util.List<UUID> getMentionedUserIds() {
        return mentionedUserIds;
    }

    public void setMentionedUserIds(java.util.List<UUID> mentionedUserIds) {
        this.mentionedUserIds = mentionedUserIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
