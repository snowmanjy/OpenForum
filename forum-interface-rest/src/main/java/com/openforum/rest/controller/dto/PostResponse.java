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
        Integer postNumber,
        Integer score,
        Integer userVote) {

    public static PostResponse from(Post post) {
        return from(post, null, 0, 0);
    }

    public static PostResponse from(Post post, String authorName) {
        return from(post, authorName, 0, 0);
    }

    public static PostResponse from(Post post, String authorName, int score, int userVote) {
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
                post.getPostNumber(),
                score,
                userVote);
    }

    /**
     * Factory method for creating PostResponse directly from PostEntity.
     * This preserves the score field which lives on the entity.
     */
    public static PostResponse fromEntity(com.openforum.infra.jpa.entity.PostEntity entity, String authorName,
            Integer userVote) {
        return new PostResponse(
                entity.getId(),
                entity.getThreadId(),
                entity.getAuthorId(),
                authorName,
                entity.getContent(),
                entity.getVersion(),
                entity.getReplyToPostId(),
                entity.getMetadata(),
                entity.getCreatedAt(),
                entity.getPostNumber(),
                entity.getScore(),
                userVote);
    }
}
