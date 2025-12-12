package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookmarks")
public class BookmarkEntity extends TenantAwareEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    public UUID getMemberId() {
        return memberId;
    }

    public void setMemberId(UUID memberId) {
        this.memberId = memberId;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }
}
