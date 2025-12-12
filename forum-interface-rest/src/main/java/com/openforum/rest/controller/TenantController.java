package com.openforum.rest.controller;

import com.openforum.application.service.TenantService;
import com.openforum.domain.aggregate.Tenant;
import com.openforum.rest.controller.dto.TenantResponse;
import com.openforum.rest.controller.dto.UpdateTenantConfigRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant management APIs")
public class TenantController {

    private static final Logger logger = LoggerFactory.getLogger(TenantController.class);

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Get Tenant", description = "Retrieves tenant details by ID")
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String tenantId) {
        return tenantService.getTenant(tenantId)
                .map(TenantResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lookup Tenant by Slug", description = "Retrieves tenant public details by slug")
    @GetMapping("/lookup")
    public ResponseEntity<com.openforum.rest.controller.dto.TenantLookupResponse> lookupTenant(
            @RequestParam String slug) {
        return tenantService.getTenantBySlug(slug)
                .map(tenant -> new com.openforum.rest.controller.dto.TenantLookupResponse(
                        java.util.UUID.fromString(tenant.getId()),
                        tenant.getName(),
                        (String) tenant.getConfig().get("primaryColor"),
                        (String) tenant.getConfig().get("logoUrl")))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update Tenant Config", description = "Updates configuration for a specific tenant")
    @PutMapping("/{tenantId}/config")
    public ResponseEntity<TenantResponse> updateTenantConfig(
            @PathVariable String tenantId,
            @RequestBody UpdateTenantConfigRequest request) {
        Tenant tenant = tenantService.updateTenantConfig(tenantId, request.config());
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @Operation(summary = "Create Tenant", description = "Provisions a new tenant")
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @RequestBody @Valid com.openforum.rest.controller.dto.CreateTenantRequest request) {
        logger.info("Received create tenant request: {}", request);
        Tenant tenant = tenantService.createTenant(request.id(), request.slug(), request.name(),
                request.externalOwnerId(), request.ownerEmail(), request.ownerName(), request.config());
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }
}
