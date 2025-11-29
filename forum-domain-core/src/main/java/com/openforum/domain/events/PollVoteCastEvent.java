package com.openforum.domain.events;

import java.time.Instant;
import java.util.UUID;

public record PollVoteCastEvent(
        UUID voteId,
        UUID pollId,
        UUID voterId,
        int optionIndex,
        Instant createdAt) {
}
