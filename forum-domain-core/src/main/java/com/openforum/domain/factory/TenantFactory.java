package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import java.util.Map;

public class TenantFactory {
    public static Tenant create(String id, String slug, String name, Map<String, Object> config) {
        return new Tenant(id, slug, name, config);
    }
}
