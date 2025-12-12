package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

public class CategoryFactory {
    public static Category create(String tenantId, String name, String slug, String description, boolean isReadOnly,
            java.util.UUID createdBy) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be empty");
        }
        return Category.create(tenantId, name, slug, description, isReadOnly, createdBy);
    }
}
