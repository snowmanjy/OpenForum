package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "polls")
public class PollEntity extends TenantAwareEntity {

    @Column(name = "post_id")
    private UUID postId;

    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> options;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "allow_multiple_votes")
    private boolean allowMultipleVotes;

    public PollEntity() {
    }

    public PollEntity(UUID id, String tenantId, UUID postId, String question, List<String> options, Instant expiresAt,
            boolean allowMultipleVotes) {
        this.id = id;
        this.tenantId = tenantId;
        this.postId = postId;
        this.question = question;
        this.options = options;
        this.expiresAt = expiresAt;
        this.allowMultipleVotes = allowMultipleVotes;
        // this.createdAt = createdAt; // Removed as it's handled by auditing and not
        // passed in constructor
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
}
