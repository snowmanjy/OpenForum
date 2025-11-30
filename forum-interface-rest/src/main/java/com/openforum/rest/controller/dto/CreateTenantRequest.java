package com.openforum.rest.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateTenantRequest(@NotNull String id, Map<String, Object> config) {
}
