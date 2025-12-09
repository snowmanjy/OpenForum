package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Post;
import com.openforum.domain.factory.PostFactory;
import com.openforum.domain.repository.PostRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Disable H2 replacement
@Testcontainers // Enable Testcontainers
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
})
@Import({ PostRepositoryImpl.class, com.openforum.infra.jpa.mapper.PostMapper.class, JpaTestConfig.class })
class PostRepositoryImplTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Autowired
        private PostRepository postRepository;

        @Autowired
        private OutboxEventJpaRepository outboxEventJpaRepository;

        @Test
        void shouldSavePostAndPublishEvent() {
                // Given
                String tenantId = "tenant-1";
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                Post post = PostFactory.create(tenantId, threadId, authorId, "Test Content", null, false,
                                java.util.List.of());
                // Manually set postNumber for test since Factory might not set it yet
                post = com.openforum.domain.aggregate.Post.builder()
                                .id(post.getId())
                                .threadId(post.getThreadId())
                                .tenantId(post.getTenantId())
                                .authorId(post.getAuthorId())
                                .content(post.getContent())
                                .replyToPostId(post.getReplyToPostId())
                                .metadata(post.getMetadata())
                                .createdAt(post.getCreatedAt())
                                .mentionedUserIds(post.getMentionedUserIds())
                                .postNumber(1)
                                .isNew(true)
                                .isBot(false)
                                .build();
                UUID postId = post.getId();

                // When
                postRepository.save(post);

                // Then
                Optional<Post> savedPost = postRepository.findById(postId);
                assertThat(savedPost).isPresent();
                assertThat(savedPost.get().getContent()).isEqualTo("Test Content");
                assertThat(savedPost.get().getPostNumber()).isEqualTo(1);

                // Verify createdAt is not null and is recent (within last minute)
                assertThat(savedPost.get().getCreatedAt()).isNotNull();
                assertThat(savedPost.get().getCreatedAt())
                                .isAfter(java.time.Instant.now().minusSeconds(60));

                List<OutboxEventEntity> events = outboxEventJpaRepository.findAll();
                assertThat(events).hasSize(1);
                assertThat(events.get(0).getType()).isEqualTo("PostCreatedEvent");
        }

        @Test
        void shouldFindPostsByThreadId() {
                // Given
                String tenantId = "tenant-1";
                UUID threadId = UUID.randomUUID();
                Post post1 = PostFactory.create(tenantId, threadId, UUID.randomUUID(), "Post 1", null, false,
                                java.util.List.of());
                Post post2 = PostFactory.create(tenantId, threadId, UUID.randomUUID(), "Post 2", null, false,
                                java.util.List.of());
                Post post3 = PostFactory.create(tenantId, UUID.randomUUID(), UUID.randomUUID(), "Other Thread", null,
                                false,
                                java.util.List.of());

                postRepository.save(post1);
                postRepository.save(post2);
                postRepository.save(post3);

                // When
                List<Post> threadPosts = postRepository.findByThreadId(threadId, 10);

                // Then
                assertThat(threadPosts).hasSize(2);
                assertThat(threadPosts).extracting(Post::getContent).containsExactlyInAnyOrder("Post 1", "Post 2");
        }
}
