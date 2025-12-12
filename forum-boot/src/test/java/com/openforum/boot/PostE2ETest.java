package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.PostEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

/**
 * E2E Integration Test for Post creation.
 * 
 * Proves that the API actually writes to a REAL PostgreSQL database,
 * not just mocks.
 */
class PostE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-post-test";

    private E2ETestDataFactory dataFactory;
    private UUID memberId;
    private UUID threadId;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member → Category → Thread chain (no post needed for creation
        // test)
        dataFactory.createTenant(TENANT_ID);
        var member = dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
        memberId = member.getId();
        var category = dataFactory.createCategory(TENANT_ID, "General");
        var thread = dataFactory.createThread(TENANT_ID, memberId, category.getId(), "Test Thread for Posts");
        threadId = thread.getId();
    }

    @Test
    void shouldCreatePost_AndPersistToDatabase() throws Exception {
        // Given: A valid JSON payload for creating a post
        String json = """
                {
                    "content": "This is an E2E test post content. Testing real database persistence!",
                    "replyToPostId": null,
                    "metadata": {},
                    "mentionedMemberIds": []
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When: POST to create post endpoint
        String responseId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/threads/{threadId}/posts", threadId.toString())
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", equalTo("This is an E2E test post content. Testing real database persistence!"))
                .extract()
                .path("id");

        // Then (DB Check): Query the repository directly
        UUID postId = UUID.fromString(responseId);
        Optional<PostEntity> postOpt = postJpaRepository.findById(postId);

        assertThat(postOpt).isPresent();
        PostEntity post = postOpt.get();

        // Verify data matches
        assertThat(post.getContent()).isEqualTo("This is an E2E test post content. Testing real database persistence!");
        assertThat(post.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(post.getAuthorId()).isEqualTo(memberId);
        assertThat(post.getThreadId()).isEqualTo(threadId);

        // Verify audit fields are populated
        assertThat(post.getCreatedAt()).isNotNull();
        // Note: createdBy requires AuditorAware configuration
    }

    @Test
    void shouldReturn401_WhenNoAuthToken() {
        // Given: A valid JSON payload but no auth token
        String json = """
                {
                    "content": "Unauthorized post content.",
                    "replyToPostId": null,
                    "metadata": {},
                    "mentionedMemberIds": []
                }
                """;

        // When/Then: POST without Authorization header should fail
        given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/threads/{threadId}/posts", threadId.toString())
                .then()
                .statusCode(401);
    }

    @Test
    void shouldCreateReplyPost_AndPersistToDatabase() throws Exception {
        // Given: First create a parent post
        String parentPostJson = """
                {
                    "content": "This is the parent post.",
                    "replyToPostId": null,
                    "metadata": {},
                    "mentionedMemberIds": []
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        String parentPostId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(parentPostJson)
                .when()
                .post("/threads/{threadId}/posts", threadId.toString())
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Then: Create a reply to that post
        String replyJson = """
                {
                    "content": "This is a reply to the parent post.",
                    "replyToPostId": "%s",
                    "metadata": {},
                    "mentionedMemberIds": []
                }
                """.formatted(parentPostId);

        String replyId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(replyJson)
                .when()
                .post("/threads/{threadId}/posts", threadId.toString())
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("content", equalTo("This is a reply to the parent post."))
                .extract()
                .path("id");

        // Verify reply is persisted with correct replyToPostId
        UUID replyUuid = UUID.fromString(replyId);
        Optional<PostEntity> replyOpt = postJpaRepository.findById(replyUuid);

        assertThat(replyOpt).isPresent();
        PostEntity reply = replyOpt.get();
        assertThat(reply.getReplyToPostId()).isEqualTo(UUID.fromString(parentPostId));
        assertThat(reply.getCreatedAt()).isNotNull();
    }
}
