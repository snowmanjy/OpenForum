package com.openforum.infra.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "members")
public class MemberEntity {

    @Id
    private UUID id;
    private String externalId;
    private String email;
    private String name;
    private String tenantId;
    private boolean isBot;
    private java.time.Instant joinedAt;
    private String role;

    public MemberEntity() {
    }

    public MemberEntity(UUID id, String externalId, String email, String name, boolean isBot, String tenantId,
            java.time.Instant joinedAt, String role) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
        this.tenantId = tenantId;
        this.joinedAt = joinedAt;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean isBot() {
        return isBot;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public java.time.Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(java.time.Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
