package com.openforum.domain.aggregate;

import java.util.UUID;

public class Category {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private final String slug;
    private final String description;
    private final java.time.Instant createdAt;
    private final java.time.Instant lastModifiedAt;
    private final UUID createdBy;
    private final UUID lastModifiedBy;
    private final boolean isReadOnly;

    // Private constructor for Factory/Reconstitution
    private Category(UUID id, String tenantId, String name, String slug, String description, boolean isReadOnly,
            java.time.Instant createdAt, java.time.Instant lastModifiedAt, UUID createdBy, UUID lastModifiedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.isReadOnly = isReadOnly;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public static Category reconstitute(UUID id, String tenantId, String name, String slug, String description,
            boolean isReadOnly, java.time.Instant createdAt, java.time.Instant lastModifiedAt, UUID createdBy,
            UUID lastModifiedBy) {
        return new Category(id, tenantId, name, slug, description, isReadOnly, createdAt, lastModifiedAt, createdBy,
                lastModifiedBy);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public java.time.Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    // Factory access
    public static Category create(String tenantId, String name, String slug, String description, boolean isReadOnly,
            UUID createdBy) {
        return new Category(UUID.randomUUID(), tenantId, name, slug, description, isReadOnly, java.time.Instant.now(),
                null, createdBy, null);
    }
}
