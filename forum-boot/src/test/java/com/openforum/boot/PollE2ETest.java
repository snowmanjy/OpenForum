package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.PollEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * E2E Integration Test for Poll operations.
 * 
 * Tests real database persistence for poll creation, voting, and retrieval.
 */
class PollE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-poll";

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

        // Create full test data chain
        testData = dataFactory.createFullTestData(TENANT_ID, EXTERNAL_ID);
        memberId = testData.member().getId();
        postId = testData.post().getId();
    }

    @Test
    void shouldCreatePoll_AndPersistToDatabase() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // Given: A valid JSON payload for creating a poll
        String expiresAt = Instant.now().plusSeconds(3600).toString();
        String json = """
                {
                    "question": "What is your favorite programming language?",
                    "options": ["Java", "Python", "JavaScript"],
                    "expiresAt": "%s",
                    "allowMultipleVotes": false
                }
                """.formatted(expiresAt);

        // When: POST to create poll endpoint
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/posts/" + postId + "/polls")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/polls/"));

        // Then (DB Check): Query the repository directly
        List<PollEntity> polls = pollJpaRepository.findAll();

        assertThat(polls).hasSize(1);
        PollEntity poll = polls.get(0);

        assertThat(poll.getPostId()).isEqualTo(postId);
        assertThat(poll.getQuestion()).isEqualTo("What is your favorite programming language?");
        assertThat(poll.getOptions()).containsExactly("Java", "Python", "JavaScript");
        assertThat(poll.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void shouldCastVote_AndPersistToDatabase() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // Given: Create a poll first
        UUID pollId = createTestPoll();

        String voteJson = """
                {
                    "optionIndex": 0
                }
                """;

        // When: POST to vote on poll
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(voteJson)
                .when()
                .post("/polls/" + pollId + "/votes")
                .then()
                .statusCode(200);

        // Then (DB Check): Query poll votes repository
        var votes = pollVoteJpaRepository.findByPollId(pollId);

        assertThat(votes).hasSize(1);
        assertThat(votes.get(0).getOptionIndex()).isEqualTo(0);
    }

    @Test
    void shouldGetPoll_WithVoteCounts() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // Given: Create a poll and cast a vote
        UUID pollId = createTestPoll();

        String voteJson = """
                {
                    "optionIndex": 1
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(voteJson)
                .when()
                .post("/polls/" + pollId + "/votes")
                .then()
                .statusCode(200);

        // When & Then: GET poll should return vote counts
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/polls/" + pollId)
                .then()
                .statusCode(200)
                .body("id", equalTo(pollId.toString()))
                .body("question", equalTo("Test Poll Question"))
                .body("options", hasItems("Option A", "Option B"))
                .body("hasVoted", equalTo(true));
    }

    @Test
    void shouldReturn401_WhenNoAuthToken() {
        // When/Then: POST without Authorization header should fail
        String json = """
                {
                    "question": "Unauthorized Poll?",
                    "options": ["Yes", "No"],
                    "expiresAt": "2099-01-01T00:00:00Z",
                    "allowMultipleVotes": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/posts/" + postId + "/polls")
                .then()
                .statusCode(401);
    }

    private UUID createTestPoll() {
        PollEntity poll = new PollEntity();
        poll.setId(UUID.randomUUID());
        poll.setTenantId(TENANT_ID);
        poll.setPostId(postId);
        poll.setQuestion("Test Poll Question");
        poll.setOptions(List.of("Option A", "Option B"));
        poll.setExpiresAt(Instant.now().plusSeconds(3600));
        poll.setAllowMultipleVotes(false);
        pollJpaRepository.save(poll);
        return poll.getId();
    }
}
