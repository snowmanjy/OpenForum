package com.openforum.domain.aggregate;

import java.util.UUID;

public class Member {
    private final UUID id;
    private final String externalId;
    private final String email;
    private final String name;
    private final boolean isBot;

    private Member(UUID id, String externalId, String email, String name, boolean isBot) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
    }

    public static Member create(String externalId, String email, String name, boolean isBot) {
        return new Member(UUID.randomUUID(), externalId, email, name, isBot);
    }

    public static Member reconstitute(UUID id, String externalId, String email, String name, boolean isBot) {
        return new Member(id, externalId, email, name, isBot);
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
}
