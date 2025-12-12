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
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public TenantService(TenantRepository tenantRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    public Optional<Tenant> getTenant(String tenantId) {
        return tenantRepository.findById(tenantId);
    }

    public Optional<Tenant> getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug);
    }

    @Transactional
    public Tenant updateTenantConfig(String tenantId, Map<String, Object> config) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // Create new tenant with updated config using factory, preserving slug and name
        Tenant updatedTenant = com.openforum.domain.factory.TenantFactory.create(tenant.getId(), tenant.getSlug(),
                tenant.getName(), config, tenant.getCreatedAt(), tenant.getCreatedBy(), tenant.getLastModifiedAt(),
                tenant.getLastModifiedBy());
        return tenantRepository.save(updatedTenant);
    }

    @Transactional
    public Tenant createTenant(String tenantId, String slug, String name, String externalOwnerId, String ownerEmail,
            String ownerName, Map<String, Object> config) {
        if (tenantRepository.findById(tenantId).isPresent()) {
            throw new IllegalArgumentException("Tenant already exists: " + tenantId);
        }
        Tenant newTenant = com.openforum.domain.factory.TenantFactory.create(tenantId, slug, name, config,
                java.time.Instant.now(), null, java.time.Instant.now(), null);
        Tenant savedTenant = tenantRepository.save(newTenant);

        eventPublisher.publishEvent(new com.openforum.application.event.TenantCreatedEvent(savedTenant.getId(),
                externalOwnerId, ownerEmail, ownerName));

        return savedTenant;
    }
}
