package com.openforum.domain.aggregate;

import java.util.UUID;

public class Category {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private final String slug;
    private final String description;
    private final boolean isReadOnly;

    // Private constructor for Factory/Reconstitution
    private Category(UUID id, String tenantId, String name, String slug, String description, boolean isReadOnly) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.isReadOnly = isReadOnly;
    }

    public static Category reconstitute(UUID id, String tenantId, String name, String slug, String description,
            boolean isReadOnly) {
        return new Category(id, tenantId, name, slug, description, isReadOnly);
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

    // Factory access
    static Category create(String tenantId, String name, String slug, String description, boolean isReadOnly) {
        return new Category(UUID.randomUUID(), tenantId, name, slug, description, isReadOnly);
    }
}
