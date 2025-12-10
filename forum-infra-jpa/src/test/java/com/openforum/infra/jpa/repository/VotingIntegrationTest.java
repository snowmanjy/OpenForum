package com.openforum.infra.jpa.repository;

import com.openforum.domain.repository.VoteRepository;
import com.openforum.infra.jpa.TestApplication;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.entity.PostVoteEntity;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@Transactional
class VotingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
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
    private VoteRepository voteRepository;

    @Autowired
    private PostVoteJpaRepository postVoteJpaRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private ThreadJpaRepository threadJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    private UUID postId;
    private UUID userId;
    private UUID threadId;
    private final String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        // Create a member
        MemberEntity member = new MemberEntity();
        member.setId(UUID.randomUUID());
        member.setExternalId("test-external-id-" + UUID.randomUUID());
        member.setName("Test User");
        member.setEmail("test-" + UUID.randomUUID() + "@example.com");
        member.setTenantId(tenantId);
        member.setJoinedAt(java.time.Instant.now());
        member.setRole("USER");
        member = memberJpaRepository.save(member);
        userId = member.getId();

        // Create a thread
        ThreadEntity thread = new ThreadEntity();
        thread.setId(UUID.randomUUID());
        thread.setTitle("Test Thread");
        thread.setTenantId(tenantId);
        thread.setAuthorId(userId);
        thread.setStatus(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        thread.setPostCount(1);
        thread = threadJpaRepository.save(thread);
        threadId = thread.getId();

        // Create a post
        PostEntity post = new PostEntity();
        post.setId(UUID.randomUUID());
        post.setThreadId(threadId);
        post.setAuthorId(userId);
        post.setTenantId(tenantId);
        post.setContent("Test post content");
        post.setScore(0);
        post = postJpaRepository.save(post);
        postId = post.getId();
    }

    @Test
    @DisplayName("New upvote increases post score by 1")
    void newUpvote_increasesScoreByOne() {
        // When
        voteRepository.save(postId, userId, tenantId, 1);
        voteRepository.updatePostScore(postId, 1);

        // Then
        PostEntity post = postJpaRepository.findById(postId).orElseThrow();
        assertThat(post.getScore()).isEqualTo(1);

        var vote = postVoteJpaRepository.findByPostIdAndUserId(postId, userId);
        assertThat(vote).isPresent();
        assertThat(vote.get().getValue()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("New downvote decreases post score by 1")
    void newDownvote_decreasesScoreByOne() {
        // When
        voteRepository.save(postId, userId, tenantId, -1);
        voteRepository.updatePostScore(postId, -1);

        // Then
        PostEntity post = postJpaRepository.findById(postId).orElseThrow();
        assertThat(post.getScore()).isEqualTo(-1);

        var vote = postVoteJpaRepository.findByPostIdAndUserId(postId, userId);
        assertThat(vote).isPresent();
        assertThat(vote.get().getValue()).isEqualTo((short) -1);
    }

    @Test
    @DisplayName("Changing upvote to downvote changes score by -2")
    void changeUpvoteToDownvote_changesScoreByMinusTwo() {
        // Given: existing upvote
        voteRepository.save(postId, userId, tenantId, 1);
        voteRepository.updatePostScore(postId, 1);

        // When: change to downvote
        voteRepository.update(postId, userId, -1);
        voteRepository.updatePostScore(postId, -2);

        // Then
        PostEntity post = postJpaRepository.findById(postId).orElseThrow();
        assertThat(post.getScore()).isEqualTo(-1);

        var vote = postVoteJpaRepository.findByPostIdAndUserId(postId, userId);
        assertThat(vote).isPresent();
        assertThat(vote.get().getValue()).isEqualTo((short) -1);
    }

    @Test
    @DisplayName("Removing upvote decreases score by 1")
    void removeUpvote_decreasesScoreByOne() {
        // Given: existing upvote
        voteRepository.save(postId, userId, tenantId, 1);
        voteRepository.updatePostScore(postId, 1);

        // When: remove vote
        voteRepository.delete(postId, userId);
        voteRepository.updatePostScore(postId, -1);

        // Then
        PostEntity post = postJpaRepository.findById(postId).orElseThrow();
        assertThat(post.getScore()).isEqualTo(0);

        var vote = postVoteJpaRepository.findByPostIdAndUserId(postId, userId);
        assertThat(vote).isEmpty();
    }

    @Test
    @DisplayName("Batch fetch user votes for multiple posts")
    void batchFetchUserVotes_returnsAllVotes() {
        // Given: create another post and vote on both
        PostEntity post2 = new PostEntity();
        post2.setId(UUID.randomUUID());
        post2.setThreadId(threadId);
        post2.setAuthorId(userId);
        post2.setTenantId(tenantId);
        post2.setContent("Second post");
        post2.setScore(0);
        post2 = postJpaRepository.save(post2);

        voteRepository.save(postId, userId, tenantId, 1);
        voteRepository.save(post2.getId(), userId, tenantId, -1);

        // When
        var votes = voteRepository.findByPostIdsAndUserId(
                java.util.List.of(postId, post2.getId()), userId);

        // Then
        assertThat(votes).hasSize(2);
    }
}
