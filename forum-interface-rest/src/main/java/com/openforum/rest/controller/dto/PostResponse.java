package com.openforum.rest.controller.dto;

import com.openforum.domain.aggregate.Post;

import java.util.Map;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID threadId,
        UUID authorId,
        String content,
        Long version,
        UUID replyToPostId,
        Map<String, Object> metadata) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getThreadId(),
                post.getAuthorId(),
                post.getContent(),
                post.getVersion(),
                post.getReplyToPostId(),
                post.getMetadata());
    }
}
