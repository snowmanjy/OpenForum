package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.factory.TenantFactory;
import com.openforum.infra.jpa.entity.TenantEntity;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public TenantEntity toEntity(Tenant domain) {
        TenantEntity entity = new TenantEntity();
        entity.setId(domain.getId());
        entity.setConfig(domain.getConfig());
        return entity;
    }

    public Tenant toDomain(TenantEntity entity) {
        return TenantFactory.create(entity.getId(), entity.getConfig());
    }
}
