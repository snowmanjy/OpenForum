package com.openforum.infra.jpa.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "post_votes")
public class PostVoteEntity extends TenantAwareEntity {

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private short value;

    public PostVoteEntity() {
    }

    public PostVoteEntity(UUID postId, UUID memberId, String tenantId, short value) {
        this.id = UUID.randomUUID();
        this.postId = postId;
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.value = value;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public void setMemberId(UUID memberId) {
        this.memberId = memberId;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }
}
