package com.openforum.infra.jpa.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "post_votes")
public class PostVoteEntity extends TenantAwareEntity {

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private short value;

    public PostVoteEntity() {
    }

    public PostVoteEntity(UUID postId, UUID userId, String tenantId, short value) {
        this.id = UUID.randomUUID();
        this.postId = postId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.value = value;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }
}
