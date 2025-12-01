package com.openforum.domain.aggregate;

import java.util.UUID;

public class Member {
    private final UUID id;
    private final String externalId;
    private final String email;
    private final String name;
    private final boolean isBot;
    private final java.time.Instant joinedAt;

    private Member(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
        this.joinedAt = joinedAt;
    }

    public static Member create(String externalId, String email, String name, boolean isBot) {
        return new Member(UUID.randomUUID(), externalId, email, name, isBot, java.time.Instant.now());
    }

    public static Member createImported(String externalId, String email, String name,
            java.time.Instant joinedAt) {
        return new Member(UUID.randomUUID(), externalId, email, name, false, joinedAt);
    }

    public static Member reconstitute(UUID id, String externalId, String email, String name, boolean isBot,
            java.time.Instant joinedAt) {
        return new Member(id, externalId, email, name, isBot, joinedAt);
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
}
