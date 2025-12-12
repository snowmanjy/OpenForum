package com.openforum.boot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import com.openforum.infra.jpa.entity.ThreadEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for public thread access without JWT token.
 * 
 * Uses E2ETestDataFactory for consistent data setup.
 */
@AutoConfigureMockMvc
class PublicThreadAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private E2ETestDataFactory dataFactory;
    private UUID threadId;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member → Category → Thread chain
        String tenantId = "public-tenant";
        dataFactory.createTenant(tenantId);
        var member = dataFactory.createMember(tenantId, "auth0|123456");
        var category = dataFactory.createCategory(tenantId, "General");
        var thread = dataFactory.createThread(tenantId, member.getId(), category.getId(), "Public Thread Title");
        threadId = thread.getId();
    }

    @Test
    void shouldAccessThreadPublicly_WithoutJwtToken() throws Exception {
        // When & Then - Access thread without authentication
        mockMvc.perform(get("/api/v1/threads/" + threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(threadId.toString()))
                .andExpect(jsonPath("$.title").value("Public Thread Title"));
    }
}
