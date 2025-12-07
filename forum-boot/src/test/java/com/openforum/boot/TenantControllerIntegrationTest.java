package com.openforum.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.rest.controller.dto.CreateTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class TenantControllerIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void shouldCreateTenant() throws Exception {
                CreateTenantRequest request = new CreateTenantRequest("tenant-integration-test", "slug-integration",
                                "Integration Tenant",
                                Map.<String, Object>of("theme", "dark"));

                mockMvc.perform(post("/api/v1/tenants")
                                .with(jwt().authorities(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "SCOPE_create:tenant")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void shouldReturn400WhenIdIsMissing() throws Exception {
                // Create request with null id
                CreateTenantRequest request = new CreateTenantRequest(null, "slug-missing-id", "Missing ID Tenant",
                                Map.<String, Object>of("theme", "dark"));

                mockMvc.perform(post("/api/v1/tenants")
                                .with(jwt().authorities(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "SCOPE_create:tenant")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnTenantId_WhenSlugExists() throws Exception {
                // Given
                String tenantId = java.util.UUID.randomUUID().toString();
                String slug = "lookup-slug";
                CreateTenantRequest request = new CreateTenantRequest(tenantId, slug, "Lookup Tenant",
                                java.util.Map.of("primaryColor", "#000000", "logoUrl", "http://logo.com"));

                mockMvc.perform(post("/api/v1/tenants")
                                .with(jwt().authorities(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "SCOPE_create:tenant")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // When & Then
                mockMvc.perform(get("/api/v1/tenants/lookup")
                                .param("slug", slug))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(tenantId))
                                .andExpect(jsonPath("$.name").value("Lookup Tenant"))
                                .andExpect(jsonPath("$.primaryColor").value("#000000"))
                                .andExpect(jsonPath("$.logoUrl").value("http://logo.com"));
        }

        @Test
        void shouldReturn404_WhenSlugIsUnknown() throws Exception {
                mockMvc.perform(get("/api/v1/tenants/lookup")
                                .param("slug", "unknown-slug"))
                                .andExpect(status().isNotFound());
        }
}
