package com.openforum.infra.jpa.entity;

import com.openforum.domain.aggregate.ThreadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "threads")
public class ThreadEntity extends TenantAwareEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "post_count", nullable = false)
    private Integer postCount = 0;

    @jakarta.persistence.Version
    @Column
    private Long version;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public ThreadStatus getStatus() {
        return status;
    }

    public void setStatus(ThreadStatus status) {
        this.status = status;
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

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

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

    @Column(name = "last_activity_at", nullable = false)
    private java.time.Instant lastActivityAt;

    public java.time.Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(java.time.Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}
