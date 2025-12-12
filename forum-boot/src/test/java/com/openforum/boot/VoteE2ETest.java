package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.entity.PostVoteEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

/**
 * E2E Integration Test for Vote functionality.
 * 
 * Proves that voting API actually writes to a REAL PostgreSQL database.
 */
class VoteE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "vote-test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-vote-test";

    private E2ETestDataFactory dataFactory;
    private E2ETestDataFactory.TestData testData;
    private UUID memberId;
    private UUID postId;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create full test data chain: Tenant → Member → Category → Thread → Post
        testData = dataFactory.createFullTestData(TENANT_ID, EXTERNAL_ID);
        memberId = testData.member().getId();
        postId = testData.post().getId();
    }

    @Test
    void shouldUpvotePost_AndPersistToDatabase() throws Exception {
        // Given: A valid upvote request
        String json = """
                {
                    "value": 1
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When: PUT to vote endpoint
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .put("/posts/{postId}/vote", postId.toString())
                .then()
                .statusCode(200)
                .body("scoreDelta", equalTo(1));

        // Then (DB Check): Verify vote record exists
        Optional<PostVoteEntity> voteOpt = postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId);
        assertThat(voteOpt).isPresent();
        PostVoteEntity vote = voteOpt.get();
        assertThat(vote.getValue()).isEqualTo((short) 1);
        assertThat(vote.getTenantId()).isEqualTo(TENANT_ID);

        // Verify post score was updated
        Optional<PostEntity> postOpt = postJpaRepository.findById(postId);
        assertThat(postOpt).isPresent();
        assertThat(postOpt.get().getScore()).isEqualTo(1);
    }

    @Test
    void shouldDownvotePost_AndPersistToDatabase() throws Exception {
        // Given: A valid downvote request
        String json = """
                {
                    "value": -1
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When: PUT to vote endpoint
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .put("/posts/{postId}/vote", postId.toString())
                .then()
                .statusCode(200)
                .body("scoreDelta", equalTo(-1));

        // Then (DB Check): Verify vote record exists with value -1
        Optional<PostVoteEntity> voteOpt = postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId);
        assertThat(voteOpt).isPresent();
        assertThat(voteOpt.get().getValue()).isEqualTo((short) -1);

        // Verify post score was updated to -1
        Optional<PostEntity> postOpt = postJpaRepository.findById(postId);
        assertThat(postOpt).isPresent();
        assertThat(postOpt.get().getScore()).isEqualTo(-1);
    }

    @Test
    void shouldRemoveVote_WhenVotingSameValueAgain() throws Exception {
        // Given: First upvote the post
        String upvoteJson = """
                {
                    "value": 1
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // First upvote
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(upvoteJson)
                .when()
                .put("/posts/{postId}/vote", postId.toString())
                .then()
                .statusCode(200)
                .body("scoreDelta", equalTo(1));

        // Verify post score is 1
        Optional<PostEntity> postAfterUpvote = postJpaRepository.findById(postId);
        assertThat(postAfterUpvote).isPresent();
        assertThat(postAfterUpvote.get().getScore()).isEqualTo(1);

        // When: Vote again with same value (should remove vote)
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(upvoteJson)
                .when()
                .put("/posts/{postId}/vote", postId.toString())
                .then()
                .statusCode(200)
                .body("scoreDelta", equalTo(-1)); // Removing upvote returns -1

        // Then (DB Check): Vote should be deleted
        Optional<PostVoteEntity> voteOpt = postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId);
        assertThat(voteOpt).isEmpty();

        // Post score should be back to 0
        Optional<PostEntity> postAfterUnvote = postJpaRepository.findById(postId);
        assertThat(postAfterUnvote).isPresent();
        assertThat(postAfterUnvote.get().getScore()).isEqualTo(0);
    }

    @Test
    void shouldReturn401_WhenNoAuthToken() {
        // Given: A valid vote request but no auth token
        String json = """
                {
                    "value": 1
                }
                """;

        // When/Then: PUT without Authorization header should fail
        given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .put("/posts/{postId}/vote", postId.toString())
                .then()
                .statusCode(401);
    }
}
