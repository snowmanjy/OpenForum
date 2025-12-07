package com.openforum.infra.jpa.entity;

import com.openforum.domain.valueobject.TargetType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "subscriptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_user_target", columnNames = { "user_id", "target_id" })
}, indexes = {
        @Index(name = "idx_subscription_target", columnList = "target_id"),
        @Index(name = "idx_subscription_user", columnList = "user_id")
})
public class SubscriptionEntity extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "target_id", nullable = false)
    private java.util.UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public java.util.UUID getUserId() {
        return userId;
    }

    public void setUserId(java.util.UUID userId) {
        this.userId = userId;
    }

    public java.util.UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(java.util.UUID targetId) {
        this.targetId = targetId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
