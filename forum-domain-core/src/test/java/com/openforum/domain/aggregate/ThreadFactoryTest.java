package com.openforum.domain.aggregate;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ThreadFactoryTest {

        @Test
        void createImported_shouldCreateThreadWithoutDomainEvents() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "tenant-123";
                String title = "Imported Thread";
                LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 0, 0);

                ThreadFactory.ImportedPostData post1 = new ThreadFactory.ImportedPostData(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Post content 1",
                                null,
                                Map.of("key", "value"),
                                false,
                                LocalDateTime.of(2023, 1, 1, 1, 0));

                ThreadFactory.ImportedPostData post2 = new ThreadFactory.ImportedPostData(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Post content 2",
                                null,
                                Map.of(),
                                true,
                                LocalDateTime.of(2023, 1, 1, 2, 0));

                // When
                Thread thread = ThreadFactory.createImported(
                                threadId,
                                tenantId,
                                authorId,
                                null,
                                title,
                                ThreadStatus.OPEN,
                                createdAt,
                                Map.of("legacy_id", "12345"),
                                List.of(post1, post2));

                // Then
                assertThat(thread.getId()).isEqualTo(threadId);
                assertThat(thread.getTenantId()).isEqualTo(tenantId);
                assertThat(thread.getAuthorId()).isEqualTo(authorId);
                assertThat(thread.getTitle()).isEqualTo(title);
                assertThat(thread.getStatus()).isEqualTo(ThreadStatus.OPEN);
                assertThat(thread.getMetadata()).containsEntry("legacy_id", "12345");
                assertThat(thread.getPosts()).hasSize(2);

                // Critical assertion: No events should be generated
                assertThat(thread.pollEvents()).isEmpty();
        }

        @Test
        void createImported_shouldReconstitutePostsWithoutEvents() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID postAuthorId = UUID.randomUUID();
                String postContent = "Historical post";

                ThreadFactory.ImportedPostData postData = new ThreadFactory.ImportedPostData(
                                UUID.randomUUID(),
                                postAuthorId,
                                postContent,
                                null,
                                Map.of(),
                                false,
                                LocalDateTime.now());

                // When
                Thread thread = ThreadFactory.createImported(
                                threadId,
                                "tenant-1",
                                UUID.randomUUID(),
                                null, // categoryId
                                "Thread Title",
                                ThreadStatus.OPEN,
                                LocalDateTime.now(),
                                Map.of(),
                                List.of(postData));

                // Then
                assertThat(thread.getPosts()).hasSize(1);
                Post post = thread.getPosts().get(0);
                assertThat(post.getThreadId()).isEqualTo(threadId);
                assertThat(post.getAuthorId()).isEqualTo(postAuthorId);
                assertThat(post.getContent()).isEqualTo(postContent);

                // Post should also not generate events
                assertThat(post.pollEvents()).isEmpty();
        }

        @Test
        void createImported_shouldHandleEmptyPostsList() {
                // Given
                UUID threadId = UUID.randomUUID();

                // When
                Thread thread = ThreadFactory.createImported(
                                threadId,
                                "tenant-1",
                                UUID.randomUUID(),
                                null,
                                "Empty Thread",
                                ThreadStatus.CLOSED,
                                LocalDateTime.now(),
                                Map.of(),
                                List.of());

                // Then
                assertThat(thread.getPosts()).isEmpty();
                assertThat(thread.pollEvents()).isEmpty();
                assertThat(thread.getStatus()).isEqualTo(ThreadStatus.CLOSED);
        }
}
