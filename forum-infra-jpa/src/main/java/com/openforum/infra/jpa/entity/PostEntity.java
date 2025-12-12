package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity extends TenantAwareEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reply_to_post_id")
    private UUID replyToPostId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mentioned_member_ids", columnDefinition = "jsonb")
    private List<UUID> mentionedMemberIds;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "post_number")
    private Integer postNumber;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "bookmark_count", nullable = false)
    private Integer bookmarkCount = 0;

    /**
     * AI-generated semantic embedding vector for similarity search.
     * Dimension: 1536 (OpenAI text-embedding-3-small standard).
     * Populated asynchronously by EmbeddingService after post creation.
     * NULL indicates embedding is pending generation.
     */
    @Column(columnDefinition = "vector")
    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    private List<Double> embedding;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @Column(name = "last_modified_at")
    private java.time.Instant lastModifiedAt;

    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    @Column(name = "created_by")
    private UUID createdBy;

    @jakarta.persistence.Version
    @Column(nullable = false)
    private Long version;

    // ... getters and setters ...

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
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

    public UUID getReplyToPostId() {
        return replyToPostId;
    }

    public void setReplyToPostId(UUID replyToPostId) {
        this.replyToPostId = replyToPostId;
    }

    public List<UUID> getMentionedMemberIds() {
        return mentionedMemberIds;
    }

    public void setMentionedMemberIds(List<UUID> mentionedMemberIds) {
        this.mentionedMemberIds = mentionedMemberIds;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getPostNumber() {
        return postNumber;
    }

    public void setPostNumber(Integer postNumber) {
        this.postNumber = postNumber;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getBookmarkCount() {
        return bookmarkCount;
    }

    public void setBookmarkCount(Integer bookmarkCount) {
        this.bookmarkCount = bookmarkCount;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public java.time.Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.time.Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public java.time.Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(java.time.Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(UUID lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
