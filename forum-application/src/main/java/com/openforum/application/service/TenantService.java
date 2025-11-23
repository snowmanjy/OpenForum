package com.openforum.application.service;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Optional<Tenant> getTenant(String tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Transactional
    public Tenant updateTenantConfig(String tenantId, Map<String, Object> config) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // Create new tenant with updated config using factory
        Tenant updatedTenant = com.openforum.domain.aggregate.TenantFactory.create(tenant.getId(), config);
        return tenantRepository.save(updatedTenant);
    }
}
