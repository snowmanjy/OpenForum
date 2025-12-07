package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;

/**
 * Base abstract class for all tenant-aware JPA entities.
 * Extends BaseEntity and adds tenantId field with Hibernate filter
 * for automatic multi-tenancy enforcement at the database layer.
 */
@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    protected String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
