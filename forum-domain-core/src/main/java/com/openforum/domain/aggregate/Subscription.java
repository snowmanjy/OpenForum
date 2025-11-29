package com.openforum.domain.aggregate;

import com.openforum.domain.valueobject.TargetType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Subscription {

    private final UUID id;
    private final String tenantId;
    private final UUID userId;
    private final UUID targetId;
    private final TargetType targetType;
    private final Instant createdAt;

    private Subscription(UUID id, String tenantId, UUID userId, UUID targetId, TargetType targetType,
            Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }

    public static Subscription create(String tenantId, UUID userId, UUID targetId, TargetType targetType) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("TargetId cannot be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("TargetType cannot be null");
        }

        return new Subscription(
                UUID.randomUUID(),
                tenantId,
                userId,
                targetId,
                targetType,
                Instant.now());
    }

    // Reconstitute from persistence
    public static Subscription reconstitute(UUID id, String tenantId, UUID userId, UUID targetId, TargetType targetType,
            Instant createdAt) {
        return new Subscription(id, tenantId, userId, targetId, targetType, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
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
}
