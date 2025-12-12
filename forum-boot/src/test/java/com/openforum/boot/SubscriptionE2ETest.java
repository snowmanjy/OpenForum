package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * E2E Integration Test for Subscription operations.
 * 
 * Tests real database persistence for subscribe/unsubscribe/list operations.
 */
class SubscriptionE2ETest extends AbstractIntegrationTest {

        private static final String TENANT_ID = "test-tenant";
        private static final String EXTERNAL_ID = "user-e2e-subscription";

        private E2ETestDataFactory dataFactory;
        private E2ETestDataFactory.TestData testData;
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

                // Create full test data chain
                testData = dataFactory.createFullTestData(TENANT_ID, EXTERNAL_ID);
                memberId = testData.member().getId();
                threadId = testData.thread().getId();
        }

        @Test
        void shouldSubscribeToThread_AndPersistToDatabase() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // When: POST to subscribe endpoint
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/threads/" + threadId + "/subscriptions")
                                .then()
                                .statusCode(200);

                // Then (DB Check): Query the repository directly
                boolean exists = subscriptionJpaRepository.existsByMemberIdAndTargetId(memberId, threadId);
                assertThat(exists).isTrue();
        }

        @Test
        void shouldUnsubscribeFromThread_AndRemoveFromDatabase() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // Given: First subscribe
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/threads/" + threadId + "/subscriptions")
                                .then()
                                .statusCode(200);

                // Verify subscription exists
                boolean beforeUnsubscribe = subscriptionJpaRepository.existsByMemberIdAndTargetId(memberId, threadId);
                assertThat(beforeUnsubscribe).isTrue();

                // When: DELETE to unsubscribe
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .delete("/threads/" + threadId + "/subscriptions")
                                .then()
                                .statusCode(204);

                // Then (DB Check): Subscription should be removed
                boolean afterUnsubscribe = subscriptionJpaRepository.existsByMemberIdAndTargetId(memberId, threadId);
                assertThat(afterUnsubscribe).isFalse();
        }

        @Test
        void shouldGetMySubscriptions_WithDatabaseData() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // Given: Subscribe to thread
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/threads/" + threadId + "/subscriptions")
                                .then()
                                .statusCode(200);

                // When & Then: GET my subscriptions should return the subscription
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .param("page", "0")
                                .param("size", "10")
                                .when()
                                .get("/subscriptions")
                                .then()
                                .statusCode(200)
                                .body("data", hasSize(1))
                                .body("total", equalTo(1));
        }

        @Test
        void shouldReturn401_WhenNoAuthToken() {
                // When/Then: POST without Authorization header should fail
                given()
                                .contentType(ContentType.JSON)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/threads/" + threadId + "/subscriptions")
                                .then()
                                .statusCode(401);
        }
}
