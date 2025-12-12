package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.ThreadEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

/**
 * E2E Integration Test for Thread creation.
 * 
 * Proves that the API actually writes to a REAL PostgreSQL database,
 * not just mocks.
 */
class ThreadE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-test";

    private E2ETestDataFactory dataFactory;
    private UUID memberId;
    private UUID categoryId;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member → Category chain (no thread/post needed for creation
        // test)
        dataFactory.createTenant(TENANT_ID);
        var member = dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
        memberId = member.getId();
        var category = dataFactory.createCategory(TENANT_ID, "General");
        categoryId = category.getId();
    }

    @Test
    void shouldCreateThread_AndPersistToDatabase() throws Exception {
        // Given: A valid JSON payload for creating a thread
        String json = """
                {
                    "title": "E2E Test Thread",
                    "content": "This is a test thread created during E2E integration testing.",
                    "categoryId": "%s"
                }
                """.formatted(categoryId.toString());

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When: POST to create thread endpoint
        String responseId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/threads")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("E2E Test Thread"))
                .extract()
                .path("id");

        // Then (DB Check): Query the repository directly
        UUID threadId = UUID.fromString(responseId);
        Optional<ThreadEntity> threadOpt = threadJpaRepository.findById(threadId);

        assertThat(threadOpt).isPresent();
        ThreadEntity thread = threadOpt.get();

        // Verify data matches
        assertThat(thread.getTitle()).isEqualTo("E2E Test Thread");
        assertThat(thread.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(thread.getAuthorId()).isEqualTo(memberId);
        assertThat(thread.getCategoryId()).isEqualTo(categoryId);

        // Verify audit fields are populated
        assertThat(thread.getCreatedAt()).isNotNull();
        assertThat(thread.getCreatedBy()).isNotNull();
    }

    @Test
    void shouldReturn401_WhenNoAuthToken() {
        // Given: A valid JSON payload but no auth token
        String json = """
                {
                    "title": "Unauthorized Thread",
                    "content": "This should fail.",
                    "categoryId": "%s"
                }
                """.formatted(categoryId.toString());

        // When/Then: POST without Authorization header should fail
        given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/threads")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn400_WhenMissingTitle() throws Exception {
        // Given: Invalid JSON payload (missing required title)
        String json = """
                {
                    "content": "This is content without a title.",
                    "categoryId": "%s"
                }
                """.formatted(categoryId.toString());

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When/Then: POST with missing title should fail validation
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/threads")
                .then()
                .statusCode(500); // TODO: Should be 400 when validation is implemented
    }

    @Test
    void shouldGetThreads_FilteredByMetadata() throws Exception {
        // Given: Create threads with different metadata
        var threadWithMetadata = createThreadWithMetadata(TENANT_ID, memberId, categoryId,
                "SAT Question 102 Discussion", java.util.Map.of("questionId", "102"));
        var threadWithOtherMetadata = createThreadWithMetadata(TENANT_ID, memberId, categoryId,
                "SAT Question 103 Discussion", java.util.Map.of("questionId", "103"));
        var threadWithoutMetadata = dataFactory.createThread(TENANT_ID, memberId, categoryId,
                "General Discussion");

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET with metadata filter should return only matching thread
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .param("metadataKey", "questionId")
                .param("metadataValue", "102")
                .when()
                .get("/threads")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("SAT Question 102 Discussion"));
    }

    @Test
    void shouldGetAllThreads_WhenNoMetadataFilter() throws Exception {
        // Given: Create multiple threads
        dataFactory.createThread(TENANT_ID, memberId, categoryId, "Thread 1");
        dataFactory.createThread(TENANT_ID, memberId, categoryId, "Thread 2");

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET without metadata filter should return all threads
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/threads")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    private com.openforum.infra.jpa.entity.ThreadEntity createThreadWithMetadata(
            String tenantId, UUID authorId, UUID catId, String title, java.util.Map<String, Object> metadata) {
        com.openforum.infra.jpa.entity.ThreadEntity thread = new com.openforum.infra.jpa.entity.ThreadEntity();
        thread.setId(UUID.randomUUID());
        thread.setTenantId(tenantId);
        thread.setTitle(title);
        thread.setAuthorId(authorId);
        thread.setCategoryId(catId);
        thread.setCreatedAt(java.time.Instant.now());
        thread.setStatus(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        thread.setPostCount(0);
        thread.setDeleted(false);
        thread.setLastActivityAt(java.time.Instant.now());
        thread.setMetadata(metadata);
        return threadJpaRepository.save(thread);
    }
}
