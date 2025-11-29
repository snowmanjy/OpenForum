package com.openforum.infra.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "polls")
public class PollEntity {

    @Id
    private UUID id;

    private String tenantId;
    private UUID postId;
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> options;

    private Instant expiresAt;
    private boolean allowMultipleVotes;
    private Instant createdAt;

    public PollEntity() {
    }

    public PollEntity(UUID id, String tenantId, UUID postId, String question, List<String> options, Instant expiresAt,
            boolean allowMultipleVotes, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.postId = postId;
        this.question = question;
        this.options = options;
        this.expiresAt = expiresAt;
        this.allowMultipleVotes = allowMultipleVotes;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isAllowMultipleVotes() {
        return allowMultipleVotes;
    }

    public void setAllowMultipleVotes(boolean allowMultipleVotes) {
        this.allowMultipleVotes = allowMultipleVotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
