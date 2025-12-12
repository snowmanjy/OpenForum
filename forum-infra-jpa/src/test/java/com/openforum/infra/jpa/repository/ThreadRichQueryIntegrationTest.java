package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.TestApplication;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.projection.ThreadWithOPProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@Transactional
class ThreadRichQueryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ThreadJpaRepository threadJpaRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    private UUID threadId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        // Create member
        MemberEntity member = new MemberEntity();
        member.setId(UUID.randomUUID());
        member.setTenantId("test-tenant");
        member.setExternalId("ext-" + UUID.randomUUID().toString());
        member.setEmail("test@example.com");
        member.setName("Test Author");
        member.setRole("USER");
        member.setCreatedAt(Instant.now());
        memberJpaRepository.save(member);
        authorId = member.getId();

        // Create thread
        ThreadEntity thread = new ThreadEntity();
        thread.setId(UUID.randomUUID());
        thread.setTenantId("test-tenant");
        thread.setTitle("Test Thread Title");
        thread.setStatus(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        thread.setAuthorId(authorId);
        thread.setPostCount(2);
        thread.setCreatedAt(Instant.now());
        thread.setLastActivityAt(Instant.now());
        threadJpaRepository.save(thread);
        threadId = thread.getId();

        // Create OP (post #1)
        PostEntity op = new PostEntity();
        op.setId(UUID.randomUUID());
        op.setTenantId("test-tenant");
        op.setThreadId(threadId);
        op.setAuthorId(authorId);
        op.setContent("This is the original post content for the thread.");
        op.setPostNumber(1);
        op.setScore(10);
        op.setCreatedAt(Instant.now());
        postJpaRepository.save(op);

        // Create reply (post #2)
        PostEntity reply = new PostEntity();
        reply.setId(UUID.randomUUID());
        reply.setTenantId("test-tenant");
        reply.setThreadId(threadId);
        reply.setAuthorId(authorId);
        reply.setContent("This is a reply.");
        reply.setPostNumber(2);
        reply.setScore(5);
        reply.setCreatedAt(Instant.now());
        postJpaRepository.save(reply);
    }

    @Test
    @DisplayName("findRichThreadById returns thread with OP content")
    void findRichThreadById_returnsOpContent() {
        // When
        Optional<ThreadWithOPProjection> result = threadJpaRepository.findRichThreadById(threadId);

        // Then
        assertThat(result).isPresent();
        ThreadWithOPProjection thread = result.get();
        assertThat(thread.getId()).isEqualTo(threadId);
        assertThat(thread.getTitle()).isEqualTo("Test Thread Title");
        assertThat(thread.getStatus()).isEqualTo("OPEN");
        assertThat(thread.getContent()).isEqualTo("This is the original post content for the thread.");
        assertThat(thread.getAuthorId()).isEqualTo(authorId);
        assertThat(thread.getPostCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("findRichThreadById returns null content when no posts exist")
    void findRichThreadById_whenNoPosts_returnsNullContent() {
        // Create thread without any posts
        ThreadEntity emptyThread = new ThreadEntity();
        emptyThread.setId(UUID.randomUUID());
        emptyThread.setTenantId("test-tenant");
        emptyThread.setTitle("Empty Thread");
        emptyThread.setStatus(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        emptyThread.setAuthorId(authorId);
        emptyThread.setPostCount(0);
        emptyThread.setCreatedAt(Instant.now());
        emptyThread.setLastActivityAt(Instant.now());
        threadJpaRepository.save(emptyThread);

        // When
        Optional<ThreadWithOPProjection> result = threadJpaRepository.findRichThreadById(emptyThread.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isNull();
    }

    @Test
    @DisplayName("findRichThreadById returns empty when thread not found")
    void findRichThreadById_whenNotFound_returnsEmpty() {
        // When
        Optional<ThreadWithOPProjection> result = threadJpaRepository.findRichThreadById(UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }
}
