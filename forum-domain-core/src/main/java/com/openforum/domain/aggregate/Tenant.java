package com.openforum.domain.aggregate;

import java.util.Map;
import java.util.Objects;

public class Tenant {
    private final String id;
    private final String slug;
    private final String name;
    private final Map<String, Object> config;

    public Tenant(String id, String slug, String name, Map<String, Object> config) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.config = config != null ? Map.copyOf(config) : Map.of();
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
}
