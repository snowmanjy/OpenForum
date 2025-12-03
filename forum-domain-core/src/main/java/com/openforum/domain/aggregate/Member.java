package com.openforum.domain.aggregate;

import java.util.UUID;

public class Member {
    private final UUID id;
    private final String externalId;
    private final String email;
    private final String name;
    private final boolean isBot;
    private final java.time.Instant joinedAt;
    private final com.openforum.domain.valueobject.MemberRole role;
    private final String tenantId;

    private Member(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt, com.openforum.domain.valueobject.MemberRole role, String tenantId) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
        this.joinedAt = joinedAt;
        this.role = role;
        this.tenantId = tenantId;
    }

    public static Member create(String externalId, String email, String name, boolean isBot, String tenantId) {
        return new Member(UUID.randomUUID(), externalId, email, name, isBot, java.time.Instant.now(),
                com.openforum.domain.valueobject.MemberRole.MEMBER, tenantId);
    }

    public static Member createWithRole(String externalId, String email, String name, boolean isBot,
            com.openforum.domain.valueobject.MemberRole role, String tenantId) {
        return new Member(UUID.randomUUID(), externalId, email, name, isBot, java.time.Instant.now(), role, tenantId);
    }

    public static Member createImported(String externalId, String email, String name,
            java.time.Instant joinedAt, String tenantId) {
        return new Member(UUID.randomUUID(), externalId, email, name, false, joinedAt,
                com.openforum.domain.valueobject.MemberRole.MEMBER, tenantId);
    }

    public static Member reconstitute(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt, com.openforum.domain.valueobject.MemberRole role, String tenantId) {
        return new Member(id, externalId, email, name, isBot, joinedAt, role, tenantId);
    }

    public Member promoteTo(com.openforum.domain.valueobject.MemberRole newRole) {
        return new Member(this.id, this.externalId, this.email, this.name, this.isBot, this.joinedAt, newRole,
                this.tenantId);
    }

    public Member updateDetails(String email, String name) {
        return new Member(this.id, this.externalId, email, name, this.isBot, this.joinedAt, this.role, this.tenantId);
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

    public java.time.Instant getJoinedAt() {
        return joinedAt;
    }

    public com.openforum.domain.valueobject.MemberRole getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }
}
