package com.openforum.infra.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "tenants")
@jakarta.persistence.EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class TenantEntity {

    @Id
    private String id;

    private String slug;

    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config;

    @org.springframework.data.annotation.CreatedDate
    @jakarta.persistence.Column(name = "created_at", updatable = false)
    private java.time.Instant createdAt;

    @org.springframework.data.annotation.CreatedBy
    @jakarta.persistence.Column(name = "created_by")
    private java.util.UUID createdBy;

    @org.springframework.data.annotation.LastModifiedDate
    @jakarta.persistence.Column(name = "last_modified_at")
    private java.time.Instant lastModifiedAt;

    @org.springframework.data.annotation.LastModifiedBy
    @jakarta.persistence.Column(name = "last_modified_by")
    private java.util.UUID lastModifiedBy;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
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
