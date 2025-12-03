package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.factory.TenantFactory;
import com.openforum.infra.jpa.entity.TenantEntity;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public TenantEntity toEntity(Tenant domain) {
        if (domain == null) {
            return null;
        }
        TenantEntity entity = new TenantEntity();
        entity.setId(domain.getId());
        entity.setSlug(domain.getSlug());
        entity.setName(domain.getName());
        entity.setConfig(domain.getConfig());
        return entity;
    }

    public Tenant toDomain(TenantEntity entity) {
        if (entity == null) {
            return null;
        }
        return TenantFactory.create(entity.getId(), entity.getSlug(), entity.getName(), entity.getConfig());
    }
}
