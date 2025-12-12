package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.PostEntity;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * E2E Integration Test for Bookmark feature.
 * Tests bookmark/unbookmark operations and bookmark count updates.
 */
class BookmarkE2ETest extends AbstractIntegrationTest {

        private static final String TENANT_ID = "test-tenant";
        private static final String EXTERNAL_ID = "user-bookmark-test";

        private E2ETestDataFactory dataFactory;
        private UUID memberId;
        private UUID categoryId;
        private UUID threadId;
        private UUID postId;

        @BeforeEach
        void setUpTestData() {
                dataFactory = new E2ETestDataFactory(
                                tenantJpaRepository,
                                memberJpaRepository,
                                categoryJpaRepository,
                                threadJpaRepository,
                                postJpaRepository);

                dataFactory.createTenant(TENANT_ID);
                var member = dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
                memberId = member.getId();
                var category = dataFactory.createCategory(TENANT_ID, "General");
                categoryId = category.getId();
                var thread = dataFactory.createThread(TENANT_ID, memberId, categoryId, "Test Thread");
                threadId = thread.getId();
                var post = dataFactory.createPost(TENANT_ID, threadId, memberId, "Test post content");
                postId = post.getId();
        }

        @Test
        void shouldBookmarkPost_AndIncrementCount() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // When: Bookmark the post
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(201);

                // Then: Verify bookmark count was incremented
                PostEntity updatedPost = postJpaRepository.findById(postId).orElseThrow();
                assertThat(updatedPost.getBookmarkCount()).isEqualTo(1);
        }

        @Test
        void shouldBookmarkPost_IdempotentOperation() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // Bookmark twice - should be idempotent
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(201);

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(201);

                // Count should still be 1 (not 2)
                PostEntity updatedPost = postJpaRepository.findById(postId).orElseThrow();
                assertThat(updatedPost.getBookmarkCount()).isEqualTo(1);
        }

        @Test
        void shouldUnbookmarkPost_AndDecrementCount() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // First bookmark
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(201);

                // Then unbookmark
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .delete("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(204);

                // Verify count is back to 0
                PostEntity updatedPost = postJpaRepository.findById(postId).orElseThrow();
                assertThat(updatedPost.getBookmarkCount()).isEqualTo(0);
        }

        @Test
        void shouldCheckBookmarkStatus() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // Initially not bookmarked
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .get("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(200)
                                .body("isBookmarked", equalTo(false));

                // Bookmark it
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .post("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(201);

                // Now should be bookmarked
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .get("/posts/" + postId + "/bookmark")
                                .then()
                                .statusCode(200)
                                .body("isBookmarked", equalTo(true));
        }

        @Test
        void shouldGetMemberBookmarks() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);

                // Create another post and bookmark both
                var post2 = dataFactory.createPost(TENANT_ID, threadId, memberId, "Second post");

                given().contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .post("/posts/" + postId + "/bookmark");

                given().contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .post("/posts/" + post2.getId() + "/bookmark");

                // Get member bookmarks
                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .get("/members/" + memberId + "/bookmarks")
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(2));
        }

        @Test
        void shouldReturn403_WhenViewingOtherUserBookmarks() throws Exception {
                String token = createValidToken(TENANT_ID, EXTERNAL_ID);
                UUID otherMemberId = UUID.randomUUID();

                given()
                                .contentType(ContentType.JSON)
                                .header("Authorization", "Bearer " + token)
                                .header("X-Tenant-ID", TENANT_ID)
                                .when()
                                .get("/members/" + otherMemberId + "/bookmarks")
                                .then()
                                .statusCode(403);
        }
}
