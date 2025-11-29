package com.openforum.domain.aggregate;

import com.openforum.domain.events.PostCreatedEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
// Added import for List

public class PostFactory {
        public static Post create(UUID threadId, UUID authorId, String content, UUID replyToPostId, boolean isBot,
                        List<UUID> mentionedUserIds) {
                if (content == null || content.isBlank()) {
                        throw new IllegalArgumentException("Post content cannot be empty");
                }

                return Post.builder()
                                .id(UUID.randomUUID())
                                .threadId(threadId)
                                .authorId(authorId)
                                .content(content)
                                .replyToPostId(replyToPostId)
                                .isNew(true)
                                .isBot(isBot)
                                .mentionedUserIds(mentionedUserIds)
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

        /**
         * Creates an imported post for bulk migration.
         * Does NOT generate domain events to prevent notification storms.
         * 
         * @param id            Pre-existing post ID from legacy system
         * @param threadId      Parent thread ID
         * @param authorId      Author UUID
         * @param content       Post content
         * @param replyToPostId Optional reply-to post ID
         * @param metadata      Post metadata (JSONB)
         * @param isBot         Whether the author is a bot
         * @param createdAt     Original creation timestamp
         * @return Post entity without domain events
         */
        public static Post createImported(
                        UUID id,
                        UUID threadId,
                        UUID authorId,
                        String content,
                        UUID replyToPostId,
                        Map<String, Object> metadata,
                        boolean isBot,
                        LocalDateTime createdAt) {
                return Post.builder()
                                .id(id)
                                .threadId(threadId)
                                .authorId(authorId)
                                .content(content)
                                .version(1L)
                                .replyToPostId(replyToPostId)
                                .metadata(metadata)
                                .isNew(false) // Critical: Do NOT generate PostCreatedEvent
                                .isBot(isBot)
                                .build();
        }
}
