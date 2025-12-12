package com.openforum.domain.aggregate;

import com.openforum.domain.valueobject.TargetType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Subscription {

    private final UUID id;
    private final String tenantId;
    private final UUID memberId;
    private final UUID targetId;
    private final TargetType targetType;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;

    private Subscription(UUID id, String tenantId, UUID memberId, UUID targetId, TargetType targetType,
            Instant createdAt, UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.memberId = memberId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    public static Subscription create(String tenantId, UUID memberId, UUID targetId, TargetType targetType) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("MemberId cannot be null");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("TargetId cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("TargetType cannot be null");
        }

        Instant now = Instant.now();
        return new Subscription(
                UUID.randomUUID(),
                tenantId,
                memberId,
                targetId,
                targetType,
                now,
                memberId,
                now,
                memberId);
    }

    // Reconstitute from persistence
    public static Subscription reconstitute(UUID id, String tenantId, UUID memberId, UUID targetId,
            TargetType targetType,
            Instant createdAt, UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        return new Subscription(id, tenantId, memberId, targetId, targetType, createdAt, createdBy, lastModifiedAt,
                lastModifiedBy);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }
}
