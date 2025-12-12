package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.UUID;

public class Tag {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private long usageCount;

    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;

    public Tag(UUID id, String tenantId, String name, long usageCount, Instant createdAt, UUID createdBy,
            Instant lastModifiedAt, UUID lastModifiedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.usageCount = usageCount;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    public static Tag reconstitute(UUID id, String tenantId, String name, long usageCount, Instant createdAt,
            UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        return new Tag(id, tenantId, name, usageCount, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public long getUsageCount() {
        return usageCount;
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
