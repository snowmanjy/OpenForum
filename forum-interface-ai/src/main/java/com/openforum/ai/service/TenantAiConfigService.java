package com.openforum.ai.service;

import com.openforum.ai.config.TenantAiConfig;
import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantAiConfigService {

    private final TenantRepository tenantRepository;

    public TenantAiConfigService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Optional<TenantAiConfig> getConfig(String tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getConfig)
                .map(TenantAiConfig::from);
    }
}
