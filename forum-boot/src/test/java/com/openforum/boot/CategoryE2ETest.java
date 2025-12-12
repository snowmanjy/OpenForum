package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.CategoryEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

/**
 * E2E Integration Test for Category CRUD operations.
 * 
 * Tests real database persistence for category creation and retrieval.
 */
class CategoryE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-category";

    private E2ETestDataFactory dataFactory;
    private UUID memberId;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member (no category for creation test)
        dataFactory.createTenant(TENANT_ID);
        var member = dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
        memberId = member.getId();
    }

    @Test
    void shouldCreateCategory_AndPersistToDatabase() throws Exception {
        // Given: A valid JSON payload for creating a category
        String json = """
                {
                    "name": "E2E Test Category",
                    "slug": "e2e-test-category",
                    "description": "Category created in E2E test",
                    "readOnly": false
                }
                """;

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When: POST to create category endpoint
        String responseId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/categories")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("E2E Test Category"))
                .body("slug", equalTo("e2e-test-category"))
                .extract()
                .path("id");

        // Then (DB Check): Query the repository directly
        UUID categoryId = UUID.fromString(responseId);
        Optional<CategoryEntity> categoryOpt = categoryJpaRepository.findById(categoryId);

        assertThat(categoryOpt).isPresent();
        CategoryEntity category = categoryOpt.get();

        // Verify data matches
        assertThat(category.getName()).isEqualTo("E2E Test Category");
        assertThat(category.getSlug()).isEqualTo("e2e-test-category");
        assertThat(category.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(category.getDescription()).isEqualTo("Category created in E2E test");
    }

    @Test
    void shouldGetCategories_WithDatabaseData() throws Exception {
        // Given: Pre-existing categories in the database
        dataFactory.createCategory(TENANT_ID, "General");
        dataFactory.createCategory(TENANT_ID, "Announcements");

        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET categories should return both
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("General", "Announcements"));
    }

    @Test
    void shouldReturn401_WhenNoAuthToken() {
        // Given: A valid JSON payload but no auth token
        String json = """
                {
                    "name": "Unauthorized Category",
                    "slug": "unauthorized-category",
                    "description": "This should fail",
                    "readOnly": false
                }
                """;

        // When/Then: POST without Authorization header should fail
        given()
                .contentType(ContentType.JSON)
                .header("X-Tenant-ID", TENANT_ID)
                .body(json)
                .when()
                .post("/categories")
                .then()
                .statusCode(401);
    }
}
