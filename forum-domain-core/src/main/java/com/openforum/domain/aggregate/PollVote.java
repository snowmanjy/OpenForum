package com.openforum.domain.aggregate;

import java.time.Instant;
import java.util.UUID;

public class PollVote {
    private final UUID id;
    private final UUID pollId;
    private final UUID voterId;
    private final int optionIndex;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant lastModifiedAt;
    private final UUID lastModifiedBy;

    private PollVote(UUID id, UUID pollId, UUID voterId, int optionIndex, Instant createdAt, UUID createdBy,
            Instant lastModifiedAt, UUID lastModifiedBy) {
        this.id = id;
        this.pollId = pollId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.lastModifiedAt = lastModifiedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    public static PollVote create(UUID pollId, UUID voterId, int optionIndex) {
        Instant now = Instant.now();
        return new PollVote(UUID.randomUUID(), pollId, voterId, optionIndex, now, voterId, now, voterId);
    }

    public static PollVote reconstitute(UUID id, UUID pollId, UUID voterId, int optionIndex, Instant createdAt,
            UUID createdBy, Instant lastModifiedAt, UUID lastModifiedBy) {
        return new PollVote(id, pollId, voterId, optionIndex, createdAt, createdBy, lastModifiedAt, lastModifiedBy);
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
