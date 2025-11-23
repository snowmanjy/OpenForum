package com.openforum.domain.aggregate;

import java.util.Map;
import java.util.UUID;

public class PostFactory {
    public static Post create(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata) {
        return Post.builder()
                .id(UUID.randomUUID())
                .threadId(threadId)
                .authorId(authorId)
                .content(content)
                .version(1L)
                .replyToPostId(replyToPostId)
                .metadata(metadata)
                .isNew(true)
                .build();
    }

    public static Post create(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata, boolean isBot) {
        return Post.builder()
                .id(UUID.randomUUID())
                .threadId(threadId)
                .authorId(authorId)
                .content(content)
                .version(1L)
                .replyToPostId(replyToPostId)
                .metadata(metadata)
                .isNew(true)
                .isBot(isBot)
                .build();
    }
}
