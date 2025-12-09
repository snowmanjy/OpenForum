package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.infra.jpa.config.JpaTestConfig;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({
        JpaTestConfig.class,
        com.openforum.infra.jpa.mapper.ThreadMapper.class,
        com.openforum.infra.jpa.mapper.PostMapper.class,
        com.openforum.infra.jpa.repository.ThreadRepositoryImpl.class,
        com.openforum.infra.jpa.repository.PostRepositoryImpl.class
})
public class ThreadCreationReproductionTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class BrokenConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            // Intentionally missing JavaTimeModule to simulate serialization failure
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private com.openforum.domain.repository.ThreadRepository threadRepository;

    @Autowired
    private com.openforum.domain.repository.PostRepository postRepository;

    @Autowired
    private ThreadJpaRepository threadJpaRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Test
    void shouldCreateThreadUsingService() {
        String tenantId = "test-tenant";
        UUID authorId = UUID.randomUUID();
        String title = "Service Created Thread";
        String content = "Service Created Content";

        // Manually instantiate ThreadService with Autowired repositories
        // Note: ThreadService is in forum-application which is a dependency.
        com.openforum.application.service.ThreadService threadService = new com.openforum.application.service.ThreadService(
                threadRepository, postRepository);

        com.openforum.domain.aggregate.Thread createdThread = threadService.createThread(tenantId, authorId, title,
                content);

        assertThat(createdThread).isNotNull();
        assertThat(createdThread.getId()).isNotNull();
        assertThat(createdThread.getTitle()).isEqualTo(title);
        assertThat(createdThread.getPostCount()).isEqualTo(1);

        // Verify Persistence
        // Flush via JPA repository because Domain Repository doesn't expose flush (it
        // relies on transaction commit)
        threadJpaRepository.flush();
        postJpaRepository.flush();

        ThreadEntity savedThread = threadJpaRepository.findById(createdThread.getId()).orElseThrow();
        assertThat(savedThread.getPostCount()).isEqualTo(1);
        assertThat(savedThread.getCreatedAt()).isNotNull();

        // Verify Post
        List<PostEntity> posts = postJpaRepository.findAll();
        assertThat(posts).hasSize(1);
        PostEntity savedPost = posts.get(0);
        assertThat(savedPost.getThreadId()).isEqualTo(savedThread.getId());
        assertThat(savedPost.getContent()).isEqualTo(content);
        assertThat(savedPost.getPostNumber()).isEqualTo(1);
        assertThat(savedPost.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldSaveThreadAndPostChecksAuditing() {
        String tenantId = "test-tenant";
        UUID authorId = UUID.randomUUID();

        // 1. Save Thread
        ThreadEntity thread = new ThreadEntity();
        thread.setId(UUID.randomUUID());
        thread.setTenantId(tenantId);
        thread.setAuthorId(authorId);
        thread.setTitle("Test Title");
        thread.setStatus(ThreadStatus.OPEN);
        thread.setPostCount(1);
        thread.setMetadata(Map.of());

        // Ensure createdAt is null initially
        assertThat(thread.getCreatedAt()).isNull();

        ThreadEntity savedThread = threadJpaRepository.save(thread);
        threadJpaRepository.flush(); // Force flush to trigger DB constraints

        assertThat(savedThread.getCreatedAt()).isNotNull();
        System.out.println("Thread CreatedAt: " + savedThread.getCreatedAt());

        // 2. Save Post
        PostEntity post = new PostEntity();
        post.setId(UUID.randomUUID());
        post.setTenantId(tenantId);
        post.setThreadId(savedThread.getId()); // Set FK
        post.setAuthorId(authorId);
        post.setContent("Content");
        post.setPostNumber(1);
        post.setMentionedUserIds(List.of());
        post.setMetadata(Map.of());

        assertThat(post.getCreatedAt()).isNull();

        PostEntity savedPost = postJpaRepository.save(post);
        postJpaRepository.flush();

        assertThat(savedPost.getCreatedAt()).isNotNull();
        System.out.println("Post CreatedAt: " + savedPost.getCreatedAt());
    }
}
