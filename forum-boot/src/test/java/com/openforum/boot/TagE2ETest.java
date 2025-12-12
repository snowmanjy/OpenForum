package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.TagEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * E2E Integration Test for Tag search operations.
 * 
 * Tests real database retrieval for tag search endpoint.
 */
class TagE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-tag";

    private E2ETestDataFactory dataFactory;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member
        dataFactory.createTenant(TENANT_ID);
        dataFactory.createMember(TENANT_ID, EXTERNAL_ID);

        // Create test tags directly
        createTag(TENANT_ID, "java", 100);
        createTag(TENANT_ID, "javascript", 50);
        createTag(TENANT_ID, "python", 30);
    }

    private TagEntity createTag(String tenantId, String name, long usageCount) {
        TagEntity tag = new TagEntity();
        tag.setId(UUID.randomUUID());
        tag.setTenantId(tenantId);
        tag.setName(name);
        tag.setUsageCount(usageCount);
        return tagJpaRepository.save(tag);
    }

    @Test
    void shouldSearchTags_WithDatabaseData() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: Search for tags starting with "java"
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", "java")
                .when()
                .get("/tags/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("java", "javascript"));
    }

    @Test
    void shouldReturnEmptyList_WhenNoTagsMatch() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: Search for non-existent tag prefix
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", "nonexistent")
                .when()
                .get("/tags/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void shouldReturnSingleTag_WithExactPrefix() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: Search for "py" should return only "python"
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .param("q", "py")
                .when()
                .get("/tags/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo("python"))
                .body("[0].usageCount", equalTo(30));
    }
}
