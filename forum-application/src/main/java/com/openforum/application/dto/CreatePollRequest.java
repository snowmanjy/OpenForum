package com.openforum.application.dto;

import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        String question,
        List<String> options,
        Instant expiresAt,
        boolean allowMultipleVotes) {
}
