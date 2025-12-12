package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Tenant {
    private final String id;
    private final String slug;
    private final String name;
    private final Map<String, Object> config;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;

    public Tenant(String id, String slug, String name, Map<String, Object> config, Instant createdAt, UUID createdBy,
            Instant lastModifiedAt, UUID lastModifiedBy) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = config != null ? Map.copyOf(config) : Map.of();
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getConfig() {
        return config;
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
