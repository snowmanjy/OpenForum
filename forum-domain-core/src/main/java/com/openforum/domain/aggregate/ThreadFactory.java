package com.openforum.domain.aggregate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ThreadFactory {

        public static Thread create(String tenantId, UUID authorId, UUID categoryId, String title,
                        Map<String, Object> metadata) {
                return Thread.builder()
                                .id(UUID.randomUUID())
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .categoryId(categoryId)
                                .title(title)
                                .metadata(metadata)
                                .isNew(true)
                                .build();
        }

        /**
         * Creates an imported thread for bulk migration.
         * Does NOT generate domain events to prevent notification storms.
         * 
         * @param id            Pre-existing thread ID from legacy system
         * @param tenantId      Tenant identifier
         * @param authorId      Author UUID
         * @param categoryId    Category UUID
         * @param title         Thread title
         * @param status        Thread status (OPEN, CLOSED, ARCHIVED)
         * @param createdAt     Original creation timestamp
         * @param metadata      Thread metadata (JSONB)
         * @param importedPosts List of posts to reconstitute with the thread
         * @return Thread aggregate without domain events
         */
        public static Thread createImported(
                        UUID id,
                        String tenantId,
                        UUID authorId,
                        UUID categoryId,
                        String title,
                        ThreadStatus status,
                        LocalDateTime createdAt,
                        Map<String, Object> metadata,
                        List<ImportedPostData> importedPosts) {

                Thread thread = Thread.builder()
                                .id(id)
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .categoryId(categoryId)
                                .title(title)
                                .status(status)
                                .metadata(metadata)
                                .version(1L)
                                .isNew(false) // Critical: Do NOT generate ThreadCreatedEvent
                                .build();

                // Reconstitute posts without events
                for (ImportedPostData postData : importedPosts) {
                        Post post = PostFactory.createImported(
                                        postData.id(),
                                        thread.getId(),
                                        postData.authorId(),
                                        postData.content(),
                                        postData.replyToPostId(),
                                        postData.metadata(),
                                        postData.isBot(),
                                        postData.createdAt());
                        thread.addImportedPost(post);
                }

                return thread;
        }

        /**
         * Data carrier for imported posts. Used to avoid coupling ThreadFactory to
         * DTOs.
         */
        public record ImportedPostData(
                        UUID id,
                        UUID authorId,
                        String content,
                        UUID replyToPostId,
                        Map<String, Object> metadata,
                        boolean isBot,
                        LocalDateTime createdAt) {
        }
}
