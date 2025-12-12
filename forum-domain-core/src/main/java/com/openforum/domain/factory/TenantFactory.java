package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class TenantFactory {
    public static Tenant create(String id, String slug, String name, Map<String, Object> config,
            Instant createdAt, UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        return new Tenant(id, slug, name, config, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
    }
}
