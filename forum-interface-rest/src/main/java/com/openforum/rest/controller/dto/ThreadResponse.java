package com.openforum.rest.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        String title,
        String status,
        String content, // OP content preview
        Instant createdAt,
        UUID authorId,
        String authorName, // API Aggregation - author name from MemberService
        int postCount) {
    // Constructor for backward compatibility (simple response)
    public ThreadResponse(UUID id, String title, String status) {
        this(id, title, status, null, null, null, null, 0);
    }
}
