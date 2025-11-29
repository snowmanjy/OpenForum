package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PollFactory {

    public Poll create(String tenantId, java.util.UUID postId, String question, List<String> options, Instant expiresAt,
            boolean allowMultipleVotes) {
        // Additional validation logic can go here (e.g. max options, max question
        // length)
        return Poll.create(tenantId, postId, question, options, expiresAt, allowMultipleVotes);
    }
}
