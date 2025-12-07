package com.openforum.rest.controller.dto;

import java.util.UUID;

public record TenantLookupResponse(
        UUID id,
        String name,
        String primaryColor,
        String logoUrl) {
}
