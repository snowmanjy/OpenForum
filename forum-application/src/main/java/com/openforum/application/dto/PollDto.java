package com.openforum.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PollDto(
        UUID id,
        UUID postId,
        String question,
        List<String> options,
        Instant expiresAt,
        boolean allowMultipleVotes,
        Instant createdAt,
        List<Integer> voteCounts, // Count per option index
        boolean hasVoted,
        List<Integer> myVotes // Option indices I voted for
) {
}
