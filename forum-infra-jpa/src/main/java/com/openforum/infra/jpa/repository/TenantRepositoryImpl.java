package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.TenantRepository;
import com.openforum.infra.jpa.entity.TenantEntity;
import com.openforum.infra.jpa.mapper.TenantMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantJpaRepository tenantJpaRepository;
    private final TenantMapper tenantMapper;

    public TenantRepositoryImpl(TenantJpaRepository tenantJpaRepository, TenantMapper tenantMapper) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.tenantMapper = tenantMapper;
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantEntity entity = tenantMapper.toEntity(tenant);
        TenantEntity savedEntity = tenantJpaRepository.save(entity);
        return tenantMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return tenantJpaRepository.findById(id)
                .map(tenantMapper::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return tenantJpaRepository.findBySlug(slug)
                .map(tenantMapper::toDomain);
    }
}
