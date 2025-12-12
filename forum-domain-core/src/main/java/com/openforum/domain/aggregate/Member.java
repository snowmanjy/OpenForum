package com.openforum.domain.aggregate;

import java.util.UUID;

public class Member {
    private final UUID id;
    private final String externalId;
    private final String email;
    private final String name;
    private final boolean isBot;
    private final java.time.Instant joinedAt;
    private final java.time.Instant createdAt;
    private final com.openforum.domain.valueobject.MemberRole role;
    private final String tenantId;
    private final String avatarUrl;
    private final int reputation;
    private final java.time.Instant lastModifiedAt;
    private final UUID createdBy;
    private final UUID lastModifiedBy;

    private Member(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt, java.time.Instant createdAt, com.openforum.domain.valueobject.MemberRole role,
            String tenantId,
            String avatarUrl, int reputation, java.time.Instant lastModifiedAt, UUID createdBy, UUID lastModifiedBy) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
        this.role = role;
        this.tenantId = tenantId;
        this.avatarUrl = avatarUrl;
        this.reputation = reputation;
        this.lastModifiedAt = lastModifiedAt;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public static Member create(String externalId, String email, String name, boolean isBot, String tenantId) {
        java.time.Instant now = java.time.Instant.now();
        return new Member(UUID.randomUUID(), externalId, email, name, isBot, now, now,
                com.openforum.domain.valueobject.MemberRole.MEMBER, tenantId, null, 0, now, null, null);
    }

    public static Member createWithRole(String externalId, String email, String name, boolean isBot,
            com.openforum.domain.valueobject.MemberRole role, String tenantId) {
        java.time.Instant now = java.time.Instant.now();
        return new Member(UUID.randomUUID(), externalId, email, name, isBot, now, now, role, tenantId,
                null, 0, now, null, null);
    }

    public static Member createImported(String externalId, String email, String name,
            java.time.Instant joinedAt, String tenantId) {
        return new Member(UUID.randomUUID(), externalId, email, name, false, joinedAt, joinedAt,
                com.openforum.domain.valueobject.MemberRole.MEMBER, tenantId, null, 0, joinedAt, null, null);
    }

    public static Member reconstitute(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt, java.time.Instant createdAt, com.openforum.domain.valueobject.MemberRole role,
            String tenantId,
            String avatarUrl, int reputation, java.time.Instant lastModifiedAt, UUID createdBy, UUID lastModifiedBy) {
        return new Member(id, externalId, email, name, isBot, joinedAt, createdAt, role, tenantId, avatarUrl,
                reputation,
                lastModifiedAt, createdBy, lastModifiedBy);
    }

    public Member promoteTo(com.openforum.domain.valueobject.MemberRole newRole) {
        return new Member(this.id, this.externalId, this.email, this.name, this.isBot, this.joinedAt, this.createdAt,
                newRole,
                this.tenantId, this.avatarUrl, this.reputation, this.lastModifiedAt, this.createdBy,
                this.lastModifiedBy);
    }

    public Member updateDetails(String email, String name, String avatarUrl) {
        return new Member(this.id, this.externalId, email, name, this.isBot, this.joinedAt, this.createdAt, this.role,
                this.tenantId,
                avatarUrl, this.reputation, this.lastModifiedAt, this.createdBy, this.lastModifiedBy);
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public int getReputation() {
        return reputation;
    }

    public java.time.Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }
}
