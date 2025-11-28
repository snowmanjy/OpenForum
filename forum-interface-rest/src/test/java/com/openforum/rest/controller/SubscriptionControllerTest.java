package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.dto.SubscriptionWithThreadDto;
import com.openforum.application.service.SubscriptionService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.JwtAuthenticationFilter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, MemberJwtAuthenticationConverter.class,
        JwtConfig.class })
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private java.security.interfaces.RSAPublicKey publicKey;

    private Member testMember;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        com.openforum.rest.context.TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void subscribe_shouldReturnOk_whenAuthenticated() throws Exception {
        // Given
        UUID threadId = UUID.randomUUID();
        doNothing().when(subscriptionService).subscribe(anyString(), any(UUID.class), any(UUID.class));

        // When & Then
        mockMvc.perform(post("/api/v1/threads/" + threadId + "/subscriptions")
                .with(authWithTenant(testMember, "default-tenant")))
                .andExpect(status().isOk());
    }

    @Test
    void unsubscribe_shouldReturnNoContent_whenAuthenticated() throws Exception {
        // Given
        UUID threadId = UUID.randomUUID();
        doNothing().when(subscriptionService).unsubscribe(anyString(), any(UUID.class), any(UUID.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/threads/" + threadId + "/subscriptions")
                .with(authWithTenant(testMember, "default-tenant")))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMySubscriptions_shouldReturnList_whenAuthenticated() throws Exception {
        // Given
        UUID threadId = UUID.randomUUID();
        SubscriptionWithThreadDto dto = new SubscriptionWithThreadDto(threadId, "Test Thread", LocalDateTime.now());

        when(subscriptionService.getSubscriptionsForUser(anyString(), any(UUID.class), anyInt(), anyInt()))
                .thenReturn(List.of(dto));
        when(subscriptionService.countSubscriptionsForUser(any(UUID.class))).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions")
                .with(authWithTenant(testMember, "default-tenant"))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].threadTitle").value("Test Thread"))
                .andExpect(jsonPath("$.total").value(1));
    }

    private RequestPostProcessor authWithTenant(Member member, String tenantId) {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken(member, null, Collections.emptyList());
            request = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .authentication(auth)
                    .postProcessRequest(request);
            com.openforum.rest.context.TenantContext.setTenantId(tenantId);
            return request;
        };
    }
}
