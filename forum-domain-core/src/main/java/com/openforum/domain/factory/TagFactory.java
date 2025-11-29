package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import java.util.UUID;

public class TagFactory {
    public static Tag create(String tenantId, String name) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return new Tag(UUID.randomUUID(), tenantId, name.toLowerCase(), 0);
    }
}
