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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
