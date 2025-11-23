package com.openforum.domain.aggregate;

import java.util.Map;
import java.util.Objects;

public class Tenant {
    private final String id;
    private final Map<String, Object> config;

    Tenant(String id, Map<String, Object> config) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.config = config != null ? Map.copyOf(config) : Map.of();
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
