package com.openforum.rest.controller.dto;

import java.util.Map;

public record UpdateTenantConfigRequest(
        Map<String, Object> config) {
}
