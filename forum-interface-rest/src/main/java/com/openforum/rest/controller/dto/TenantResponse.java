package com.openforum.rest.controller.dto;

import com.openforum.domain.aggregate.Tenant;

import java.util.Map;

public record TenantResponse(
        String id,
        Map<String, Object> config) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getConfig());
    }
}
