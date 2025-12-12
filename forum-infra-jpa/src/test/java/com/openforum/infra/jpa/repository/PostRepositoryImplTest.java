package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Post;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostRepositoryImplTest {

        @Mock
        private PostJpaRepository postJpaRepository;
        @Mock
        private OutboxEventJpaRepository outboxEventJpaRepository;

        // Using real mapper for strict validation
        private PostMapper postMapper = new PostMapper();
        @Mock
        private ObjectMapper objectMapper;

        private PostRepositoryImpl postRepository;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                postRepository = new PostRepositoryImpl(postJpaRepository, outboxEventJpaRepository, postMapper,
                                objectMapper);
        }

        @Test
        void save_NewPost_ShouldPersistCorrectly() {
                // Arrange
                Post post = Post.builder()
                                .id(UUID.randomUUID())
                                .threadId(UUID.randomUUID())
                                .tenantId("tenant-1")
                                .authorId(UUID.randomUUID())
                                .content("New Content")
                                .version(1L)
                                .isNew(true)
                                .build();

                when(postJpaRepository.findById(post.getId())).thenReturn(Optional.empty());

                // Act
                postRepository.save(post);

                // Assert
                ArgumentCaptor<PostEntity> entityCaptor = ArgumentCaptor.forClass(PostEntity.class);
                verify(postJpaRepository).save(entityCaptor.capture());

                PostEntity savedEntity = entityCaptor.getValue();
                assertEquals(post.getContent(), savedEntity.getContent());
                // Normal save has score 0 by default entity initialization
                assertEquals(0, savedEntity.getScore());
        }

        @Test
        void save_ExistingPost_ShouldPreserveScore_AndUpdateContent() {
                // Arrange
                UUID postId = UUID.randomUUID();

                // Existing entity in DB has a score of 100
                PostEntity existingEntity = new PostEntity();
                existingEntity.setId(postId);
                existingEntity.setThreadId(UUID.randomUUID());
                existingEntity.setTenantId("tenant-1");
                existingEntity.setAuthorId(UUID.randomUUID());
                existingEntity.setContent("Old Content");
                existingEntity.setScore(100);
                existingEntity.setEmbedding(List.of(0.1, 0.2, 0.3)); // Existing embedding
                existingEntity.setDeletedAt(Instant.parse("2023-01-01T00:00:00Z")); // Existing deletedAt

                when(postJpaRepository.findById(postId)).thenReturn(Optional.of(existingEntity));

                // Domain object coming in with updated content
                // Domain now carries the score explicitly (100)
                Post updatedDomain = Post.builder()
                                .id(postId)
                                .threadId(existingEntity.getThreadId())
                                .tenantId("tenant-1")
                                .authorId(existingEntity.getAuthorId())
                                .content("Updated Content")
                                .version(2L)
                                .score(100)
                                // Embedding NOT set in domain (null) -> Should be preserved by Mapper
                                // conditional
                                // DeletedAt IS set in domain (must be carried over)
                                .deletedAt(existingEntity.getDeletedAt())
                                .build();

                // Act
                postRepository.save(updatedDomain);

                // Assert
                ArgumentCaptor<PostEntity> entityCaptor = ArgumentCaptor.forClass(PostEntity.class);
                verify(postJpaRepository).save(entityCaptor.capture());

                PostEntity savedEntity = entityCaptor.getValue();

                // 1. Verify content IS updated
                assertEquals("Updated Content", savedEntity.getContent());

                // 2. CRITICAL: Verify score matches domain score
                assertEquals(100, savedEntity.getScore());

                // 3. Verify Embedding preserved (via conditional update in Mapper)
                assertEquals(3, savedEntity.getEmbedding().size());
                assertEquals(0.1, savedEntity.getEmbedding().get(0));

                // 4. Verify DeletedAt preserved (via explicit domain carry-over)
                assertEquals(existingEntity.getDeletedAt(), savedEntity.getDeletedAt());

                // 5. Verify other existing fields
                assertEquals(existingEntity.getThreadId(), savedEntity.getThreadId());
                assertEquals(existingEntity.getAuthorId(), savedEntity.getAuthorId());
        }
}
