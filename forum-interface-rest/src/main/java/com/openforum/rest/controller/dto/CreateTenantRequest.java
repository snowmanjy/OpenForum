package com.openforum.rest.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateTenantRequest(
        @NotNull String id,
        @NotNull String slug,
        @NotNull String name,
        @NotNull String externalOwnerId,
        @NotNull String ownerEmail,
        String ownerName,
        Map<String, Object> config) {
}
