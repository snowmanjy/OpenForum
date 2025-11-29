package com.openforum.infra.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "private_threads")
public class PrivateThreadEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String subject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "private_thread_participants", joinColumns = @JoinColumn(name = "private_thread_id"))
    @Column(name = "participant_id")
    private Set<UUID> participantIds = new HashSet<>();

    public PrivateThreadEntity() {
    }

    public PrivateThreadEntity(UUID id, String tenantId, String subject, Instant createdAt, Instant lastActivityAt,
            Set<UUID> participantIds) {
        this.id = id;
        this.tenantId = tenantId;
        this.subject = subject;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.participantIds = participantIds;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Set<UUID> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(Set<UUID> participantIds) {
        this.participantIds = participantIds;
    }
}
