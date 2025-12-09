package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_votes")
@jakarta.persistence.EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class PollVoteEntity extends BaseEntity {

    @Column(name = "poll_id")
    private UUID pollId;

    @Column(name = "voter_id")
    private UUID voterId;

    @Column(name = "option_index")
    private int optionIndex;

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public PollVoteEntity() {
    }

    public PollVoteEntity(UUID id, UUID pollId, UUID voterId, int optionIndex) {
        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
        // this.createdAt = createdAt;
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
