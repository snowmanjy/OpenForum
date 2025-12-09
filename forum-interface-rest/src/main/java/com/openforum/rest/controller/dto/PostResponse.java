package com.openforum.rest.controller.dto;

import com.openforum.domain.aggregate.Post;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID threadId,
        UUID authorId,
        String authorName, // API Aggregation - author name from MemberService
        String content,
        Long version,
        UUID replyToPostId,
        Map<String, Object> metadata,
        Instant createdAt,
        Integer postNumber) {

    public static PostResponse from(Post post) {
        return from(post, null);
    }

    public static PostResponse from(Post post, String authorName) {
        return new PostResponse(
                post.getId(),
                post.getThreadId(),
                post.getAuthorId(),
                authorName,
                post.getContent(),
                post.getVersion(),
                post.getReplyToPostId(),
                post.getMetadata(),
                post.getCreatedAt(),
                post.getPostNumber());
    }
}
