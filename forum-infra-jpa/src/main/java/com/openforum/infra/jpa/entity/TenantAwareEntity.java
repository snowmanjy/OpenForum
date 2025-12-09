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
@jakarta.persistence.EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    protected String tenantId;

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    protected java.time.Instant createdAt;

    @org.springframework.data.annotation.CreatedBy
    @Column(name = "created_by")
    protected java.util.UUID createdBy;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "last_modified_at")
    protected java.time.Instant lastModifiedAt;

    @org.springframework.data.annotation.LastModifiedBy
    @Column(name = "last_modified_by")
    protected java.util.UUID lastModifiedBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public java.util.UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(java.util.UUID createdBy) {
        this.createdBy = createdBy;
    }

    public java.time.Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(java.time.Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public java.util.UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(java.util.UUID lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
