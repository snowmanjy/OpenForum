package com.openforum.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for thread creation.
 * 
 * Uses E2ETestDataFactory for consistent data setup.
 */
@AutoConfigureMockMvc
class ThreadCreationEndToEndTest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String EXTERNAL_ID = "auth0|55555";
    private static final String MEMBER_NAME = "Creator";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private E2ETestDataFactory dataFactory;

    @BeforeEach
    void setUpTestData() {
        dataFactory = new E2ETestDataFactory(
                tenantJpaRepository,
                memberJpaRepository,
                categoryJpaRepository,
                threadJpaRepository,
                postJpaRepository);

        // Create Tenant → Member (no Thread needed, we're testing creation)
        dataFactory.createTenant(TENANT_ID);
        dataFactory.createMember(TENANT_ID, EXTERNAL_ID);
    }

    @Test
    void shouldCreateThreadSuccessfully() throws Exception {
        // Generate Token
        String token = createValidToken(TENANT_ID, EXTERNAL_ID);

        // Prepare Request
        CreateThreadRequest request = new CreateThreadRequest(
                "E2E Title",
                "E2E Content",
                null);

        // Perform Request with assertions
        mockMvc.perform(post("/api/v1/threads")
                .header("X-Tenant-ID", TENANT_ID)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("E2E Title"))
                .andExpect(jsonPath("$.content").value("E2E Content"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.authorName").value("Test User " + EXTERNAL_ID)) // From factory
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.postCount").value(1))
                .andExpect(jsonPath("$.authorId").isNotEmpty());
    }
}
