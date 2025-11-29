package com.openforum.infra.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_votes")
public class PollVoteEntity {

    @Id
    private UUID id;

    private UUID pollId;
    private UUID voterId;
    private int optionIndex;
    private Instant createdAt;

    public PollVoteEntity() {
    }

    public PollVoteEntity(UUID id, UUID pollId, UUID voterId, int optionIndex, Instant createdAt) {
        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPollId() {
        return pollId;
    }

    public void setPollId(UUID pollId) {
        this.pollId = pollId;
    }

    public UUID getVoterId() {
        return voterId;
    }

    public void setVoterId(UUID voterId) {
        this.voterId = voterId;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public void setOptionIndex(int optionIndex) {
        this.optionIndex = optionIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
