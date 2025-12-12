package com.openforum.infra.jpa.entity;

import com.openforum.domain.valueobject.TargetType;
import jakarta.persistence.*;

@Entity
@Table(name = "subscriptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_member_target", columnNames = { "member_id", "target_id" })
}, indexes = {
        @Index(name = "idx_subscription_target", columnList = "target_id"),
        @Index(name = "idx_subscription_member", columnList = "member_id")
})
public class SubscriptionEntity extends TenantAwareEntity {

    @Column(name = "member_id", nullable = false)
    private java.util.UUID memberId;

    @Column(name = "target_id", nullable = false)
    private java.util.UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    public java.util.UUID getMemberId() {
        return memberId;
    }

    public void setMemberId(java.util.UUID memberId) {
        this.memberId = memberId;
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
}
