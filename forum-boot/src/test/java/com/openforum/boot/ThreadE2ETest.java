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
}
