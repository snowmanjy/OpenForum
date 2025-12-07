package com.openforum.infra.jpa.entity;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Base abstract class for all JPA entities.
 * Provides the common UUID primary key field.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    protected UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
