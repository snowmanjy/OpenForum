package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import java.util.UUID;

import java.time.Instant;

public class TagFactory {
    public static Tag create(String tenantId, String name, UUID createdBy) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        Instant now = Instant.now();
        return new Tag(UUID.randomUUID(), tenantId, name.toLowerCase(), 0, now, createdBy, now, createdBy);
    }
}
