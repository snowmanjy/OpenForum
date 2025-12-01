package com.openforum.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.admin.integration.TestApplication;
import com.openforum.admin.dto.BulkImportRequest;
import com.openforum.admin.dto.ImportPostDto;
import com.openforum.admin.dto.ImportThreadDto;
import com.openforum.domain.aggregate.ThreadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
class BulkImportIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void shouldImportThreadsAndGenerateSyncEventsOnly() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "tenant-import-test";

                // Create Tenant
                jdbcTemplate.update("INSERT INTO tenants (id, config) VALUES (?, ?::jsonb)", tenantId, "{}");

                // Create Author
                jdbcTemplate.update(
                                "INSERT INTO members (id, external_id, email, name, is_bot, reputation) VALUES (?, ?, ?, ?, ?, ?)",
                                authorId, "ext-" + authorId, "test@example.com", "Test User", false, 0);

                ImportPostDto postDto = new ImportPostDto(
                                UUID.randomUUID(),
                                authorId,
                                "Imported content",
                                null,
                                Map.of(),
                                false,
                                Instant.now());

                ImportThreadDto threadDto = new ImportThreadDto(
                                threadId,
                                tenantId,
                                authorId,
                                null, // categoryId - not required
                                "Migrated Thread Title",
                                ThreadStatus.OPEN,
                                Instant.now(),
                                Map.of("legacy_id", "12345"),
                                List.of(postDto));

                BulkImportRequest request = new BulkImportRequest(List.of(threadDto));

                // When
                mockMvc.perform(post("/admin/v1/bulk/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Then

                // 1. Verify Thread Persistence
                Integer threadCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM threads WHERE id = ?", Integer.class, threadId);
                assertThat(threadCount).isEqualTo(1);

                // 2. Verify Post Persistence
                Integer postCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM posts WHERE thread_id = ?", Integer.class, threadId);
                assertThat(postCount).isEqualTo(1);

                // 3. Verify ThreadImportedEvent exists (Sync Event)
                Integer importedEventCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM outbox_events WHERE type = 'ThreadImportedEvent' AND payload::text LIKE ?",
                                Integer.class, "%Migrated Thread Title%");
                assertThat(importedEventCount).as("Should generate ThreadImportedEvent").isEqualTo(1);

                // 4. Verify ThreadCreatedEvent does NOT exist (Domain Event)
                Integer createdEventCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM outbox_events WHERE type = 'ThreadCreatedEvent' AND payload::text LIKE ?",
                                Integer.class, "%Migrated Thread Title%");
                assertThat(createdEventCount).as("Should NOT generate ThreadCreatedEvent").isEqualTo(0);
        }

        @Test
        void shouldImportThreadsWithNullCategoryId() throws Exception {
                // Given: A thread with null categoryId (uncategorized thread)
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "tenant-null-category-test";

                // Create Tenant
                jdbcTemplate.update("INSERT INTO tenants (id, config) VALUES (?, ?::jsonb)", tenantId, "{}");

                // Create Author
                jdbcTemplate.update(
                                "INSERT INTO members (id, external_id, email, name, is_bot, reputation) VALUES (?, ?, ?, ?, ?, ?)",
                                authorId, "ext-" + authorId, "test@example.com", "Test User", false, 0);

                ImportPostDto postDto = new ImportPostDto(
                                UUID.randomUUID(),
                                authorId,
                                "Post in uncategorized thread",
                                null,
                                Map.of(),
                                false,
                                Instant.now());

                ImportThreadDto threadDto = new ImportThreadDto(
                                threadId,
                                tenantId,
                                authorId,
                                null, // categoryId is null - uncategorized thread
                                "Uncategorized Thread",
                                ThreadStatus.OPEN,
                                Instant.now(),
                                Map.of(),
                                List.of(postDto));

                BulkImportRequest request = new BulkImportRequest(List.of(threadDto));

                // When: Import the thread
                mockMvc.perform(post("/admin/v1/bulk/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Then: Verify thread was saved with null categoryId
                Integer threadCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM threads WHERE id = ? AND category_id IS NULL",
                                Integer.class, threadId);
                assertThat(threadCount).as("Thread should be saved with null categoryId").isEqualTo(1);

                // Verify post was also saved
                Integer postCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM posts WHERE thread_id = ?", Integer.class, threadId);
                assertThat(postCount).isEqualTo(1);
                assertThat(postCount).isEqualTo(1);
        }

        @Test
        void shouldImportSaaSRequestPayload() throws Exception {
                // Given: The exact payload from SaaS team
                // We need to ensure the tenant exists, but we intentionally DON'T create the
                // author
                // to see if that causes the 500 error (FK violation).

                String tenantId = "my-final-community-15";
                UUID authorId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
                UUID threadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
                UUID postId = UUID.fromString("d325e447-4e5f-4668-a474-c8d300f31f00");

                // Create Tenant (since they said it exists)
                jdbcTemplate.update("INSERT INTO tenants (id, config) VALUES (?, ?::jsonb)",
                                tenantId, "{}");

                ImportPostDto postDto = new ImportPostDto(
                                postId,
                                authorId,
                                "This is the first post in the migrated thread.",
                                null,
                                Map.of("likes", 5),
                                false,
                                Instant.parse("2023-10-27T10:00:00Z"));

                ImportThreadDto threadDto = new ImportThreadDto(
                                threadId,
                                tenantId,
                                authorId,
                                null,
                                "Migration Test Thread",
                                ThreadStatus.OPEN,
                                Instant.parse("2023-10-27T10:00:00Z"),
                                Map.of("legacyId", "12345", "importedFrom", "vBulletin"),
                                List.of(postDto));

                BulkImportRequest request = new BulkImportRequest(List.of(threadDto));

                // When: Import is called
                // We expect this to fail with 400 Bad Request because the author does not exist
                mockMvc.perform(post("/admin/v1/bulk/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
