package com.openforum.rest.controller;

import com.openforum.application.service.TenantService;
import com.openforum.domain.aggregate.Tenant;
import com.openforum.rest.controller.dto.TenantResponse;
import com.openforum.rest.controller.dto.UpdateTenantConfigRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenant(tenantId)
                .map(TenantResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{tenantId}/config")
    public ResponseEntity<TenantResponse> updateTenantConfig(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantConfigRequest request) {
        Tenant tenant = tenantService.updateTenantConfig(tenantId, request.config());
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }
}
