package com.openforum.domain.aggregate;

import java.util.UUID;

public class Tag {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private long usageCount;

    Tag(UUID id, String tenantId, String name, long usageCount) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.usageCount = usageCount;
    }

    public static Tag reconstitute(UUID id, String tenantId, String name, long usageCount) {
        return new Tag(id, tenantId, name, usageCount);
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
}
