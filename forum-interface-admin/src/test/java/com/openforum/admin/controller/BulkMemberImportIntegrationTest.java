package com.openforum.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.admin.dto.MemberImportRequest;
import com.openforum.admin.integration.TestApplication;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@AutoConfigureMockMvc
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
})
class BulkMemberImportIntegrationTest {

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
        void shouldImportMembersAndHandleIdempotency() throws Exception {
                // Given
                String tenantId = "tenant-member-import";
                String externalId1 = "ext-1";
                String externalId2 = "ext-2";
                Instant joinedAt = Instant.now();

                // Create Tenant
                jdbcTemplate.update("INSERT INTO tenants (id, config) VALUES (?, ?::jsonb)", tenantId, "{}");

                // Pre-create one member to test idempotency
                UUID existingMemberId = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO members (id, external_id, email, name, is_bot, reputation, joined_at, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                existingMemberId, externalId1, "existing@example.com", "Existing User", false, 0,
                                java.sql.Timestamp.from(joinedAt), tenantId);

                MemberImportRequest.MemberImportItem item1 = new MemberImportRequest.MemberImportItem(
                                "corr-1", externalId1, "existing@example.com", "Existing User", joinedAt);
                MemberImportRequest.MemberImportItem item2 = new MemberImportRequest.MemberImportItem(
                                "corr-2", externalId2, "new@example.com", "New User", joinedAt);

                MemberImportRequest request = new MemberImportRequest(tenantId, List.of(item1, item2));

                // When
                mockMvc.perform(post("/admin/v1/bulk/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.importedCount").value(1)) // Only 1 new member imported
                                .andExpect(jsonPath("$.correlationIdMap.*", hasSize(2)))
                                .andExpect(jsonPath("$.correlationIdMap.corr-1").value(existingMemberId.toString()));

                // Then
                // Verify new member persistence
                Integer memberCount = jdbcTemplate.queryForObject(
                                "SELECT count(*) FROM members WHERE external_id = ?", Integer.class, externalId2);
                assertThat(memberCount).isEqualTo(1);

                // Verify joined_at persistence
                java.sql.Timestamp savedJoinedAt = jdbcTemplate.queryForObject(
                                "SELECT joined_at FROM members WHERE external_id = ?", java.sql.Timestamp.class,
                                externalId2);
                assertThat(savedJoinedAt).isNotNull();
                // Allow small difference due to DB precision
                assertThat(savedJoinedAt.toInstant()).isBetween(joinedAt.minusMillis(100), joinedAt.plusMillis(100));
        }
}
