package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.UUID;

public class PollVote {
    private final UUID id;
    private final UUID pollId;
    private final UUID voterId;
    private final int optionIndex;
    private final Instant createdAt;

    private PollVote(UUID id, UUID pollId, UUID voterId, int optionIndex, Instant createdAt) {
        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
        this.createdAt = createdAt;
    }

    public static PollVote create(UUID pollId, UUID voterId, int optionIndex) {
        return new PollVote(UUID.randomUUID(), pollId, voterId, optionIndex, Instant.now());
    }

    public static PollVote reconstitute(UUID id, UUID pollId, UUID voterId, int optionIndex, Instant createdAt) {
        return new PollVote(id, pollId, voterId, optionIndex, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPollId() {
        return pollId;
    }

    public UUID getVoterId() {
        return voterId;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
