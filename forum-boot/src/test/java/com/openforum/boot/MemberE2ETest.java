package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * E2E Integration Test for Member API operations.
 * 
 * Tests real database retrieval for member lookup endpoints.
 */
class MemberE2ETest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "user-e2e-member";

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

        // Create Tenant → Member
        dataFactory.createTenant(TENANT_ID);
        var member = dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
        memberId = member.getId();
    }

    @Test
    void shouldGetMemberByExternalId_WhenExists() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET member by external ID
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/members/external/" + EXTERNAL_ID)
                .then()
                .statusCode(200)
                .body("id", equalTo(memberId.toString()))
                .body("externalId", equalTo(EXTERNAL_ID))
                .body("email", equalTo(EXTERNAL_ID + "@test.com"));
    }

    @Test
    void shouldGetMemberById_WhenExists() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET member by internal ID
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/members/" + memberId)
                .then()
                .statusCode(200)
                .body("id", equalTo(memberId.toString()))
                .body("externalId", equalTo(EXTERNAL_ID));
    }

    @Test
    void shouldReturn404_WhenMemberNotFound() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);
        UUID nonExistentId = UUID.randomUUID();

        // When & Then: GET non-existent member should return 404
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/members/" + nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn404_WhenExternalIdNotFound() throws Exception {
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // When & Then: GET non-existent external ID should return 404
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-ID", TENANT_ID)
                .when()
                .get("/members/external/non-existent-external-id")
                .then()
                .statusCode(404);
    }
}
