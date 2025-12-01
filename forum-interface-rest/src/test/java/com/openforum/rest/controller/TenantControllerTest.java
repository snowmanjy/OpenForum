package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.TenantService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.JwtAuthenticationFilter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import com.openforum.rest.context.TenantContext;
import com.openforum.rest.controller.dto.UpdateTenantConfigRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, MemberJwtAuthenticationConverter.class,
        JwtConfig.class })
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private java.security.interfaces.RSAPublicKey publicKey;

    @BeforeEach
    void setUp() {
        // No setup needed for member as we mock it in auth
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTenant_shouldReturnTenant_whenExists() throws Exception {
        // Given
        String tenantId = "tenant-1";
        Tenant tenant = com.openforum.domain.factory.TenantFactory.create(tenantId, Map.of("key", "value"));
        when(tenantService.getTenant(tenantId)).thenReturn(Optional.of(tenant));

        // When & Then
        mockMvc.perform(get("/api/v1/tenants/" + tenantId)
                .with(authWithTenant("user-1", tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.config.key").value("value"));
    }

    @Test
    void getTenant_shouldReturnNotFound_whenDoesNotExist() throws Exception {
        // Given
        String tenantId = "tenant-1";
        when(tenantService.getTenant(tenantId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/tenants/" + tenantId)
                .with(authWithTenant("user-1", tenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTenantConfig_shouldReturnUpdatedTenant() throws Exception {
        // Given
        String tenantId = "tenant-1";
        Map<String, Object> newConfig = Map.of("key", "new");
        UpdateTenantConfigRequest request = new UpdateTenantConfigRequest(newConfig);

        Tenant updatedTenant = com.openforum.domain.factory.TenantFactory.create(tenantId, newConfig);
        when(tenantService.updateTenantConfig(eq(tenantId), any())).thenReturn(updatedTenant);

        // When & Then
        mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/config")
                .with(authWithTenant("user-1", tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.config.key").value("new"));
    }

    @Test
    void createTenant_shouldReturnCreatedTenant() throws Exception {
        // Given
        String tenantId = "new-tenant";
        Map<String, Object> config = Map.of("key", "value");
        com.openforum.rest.controller.dto.CreateTenantRequest request = new com.openforum.rest.controller.dto.CreateTenantRequest(
                tenantId, config);

        Tenant createdTenant = com.openforum.domain.factory.TenantFactory.create(tenantId, config);
        when(tenantService.createTenant(eq(tenantId), any())).thenReturn(createdTenant);

        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/tenants")
                .with(authWithTenant("user-1", "default-tenant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.config.key").value("value"));
    }

    private RequestPostProcessor authWithTenant(String userId, String tenantId) {
        return request -> {
            Member member = Member.reconstitute(
                    UUID.randomUUID(), "ext-" + userId, "test@example.com", "Test User", false,
                    java.time.Instant.now());

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    member, null, Collections.emptyList());

            request = SecurityMockMvcRequestPostProcessors
                    .authentication(auth)
                    .postProcessRequest(request);

            TenantContext.setTenantId(tenantId);
            return request;
        };
    }
}
