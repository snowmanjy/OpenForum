package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "members")
public class MemberEntity extends TenantAwareEntity {

    @Column(name = "external_id")
    private String externalId;

    private String email;
    private String name;

    @Column(name = "is_bot")
    private boolean isBot;

    @Column(name = "joined_at")
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

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
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
