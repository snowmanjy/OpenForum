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
    private boolean isBot;

    public MemberEntity() {
    }

    public MemberEntity(UUID id, String externalId, String email, String name, boolean isBot) {
        this.id = id;
        this.externalId = externalId;
        this.email = email;
        this.name = name;
        this.isBot = isBot;
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
