package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.SubscriptionService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.HybridJwtAuthenticationConverter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@Import({ SecurityConfig.class, HybridJwtAuthenticationConverter.class, MemberJwtAuthenticationConverter.class,
                JwtConfig.class })
class SubscriptionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private SubscriptionService subscriptionService;

        @MockitoBean
        private MemberRepository memberRepository;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private Member testMember;
        private UUID memberId;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
                memberId = UUID.randomUUID();
                testMember = Member.reconstitute(memberId, "ext-123", "test@example.com", "Test User", false,
                                java.time.Instant.now(), java.time.Instant.now(),
                                com.openforum.domain.valueobject.MemberRole.MEMBER,
                                "default-tenant", null, 0, null, null, null);
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
                com.openforum.domain.context.TenantContext.clear();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        void subscribe_shouldReturnOk_whenAuthenticated() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                doNothing().when(subscriptionService).subscribe(anyString(), any(UUID.class), any(UUID.class),
                                any(com.openforum.domain.valueobject.TargetType.class));

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
        void subscribeCategory_shouldReturnOk_whenAuthenticated() throws Exception {
                // Given
                UUID categoryId = UUID.randomUUID();
                doNothing().when(subscriptionService).subscribe(anyString(), any(UUID.class), any(UUID.class),
                                any(com.openforum.domain.valueobject.TargetType.class));

                // When & Then
                mockMvc.perform(post("/api/v1/categories/" + categoryId + "/subscriptions")
                                .with(authWithTenant(testMember, "default-tenant")))
                                .andExpect(status().isOk());
        }

        @Test
        void unsubscribeCategory_shouldReturnNoContent_whenAuthenticated() throws Exception {
                // Given
                UUID categoryId = UUID.randomUUID();
                doNothing().when(subscriptionService).unsubscribe(anyString(), any(UUID.class), any(UUID.class));

                // When & Then
                mockMvc.perform(delete("/api/v1/categories/" + categoryId + "/subscriptions")
                                .with(authWithTenant(testMember, "default-tenant")))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getMySubscriptions_shouldReturnList_whenAuthenticated() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.application.dto.SubscriptionDto dto = new com.openforum.application.dto.SubscriptionDto(
                                threadId, com.openforum.domain.valueobject.TargetType.THREAD, "Test Thread",
                                Instant.now());

                when(subscriptionService.getSubscriptionsForMember(anyString(), any(UUID.class), anyInt(), anyInt()))
                                .thenReturn(List.of(dto));
                when(subscriptionService.countSubscriptionsForMember(any(UUID.class))).thenReturn(1L);

                // When & Then
                mockMvc.perform(get("/api/v1/subscriptions")
                                .with(authWithTenant(testMember, "default-tenant"))
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].title").value("Test Thread"))
                                .andExpect(jsonPath("$.total").value(1));
        }

        private RequestPostProcessor authWithTenant(Member member, String tenantId) {
                return request -> {
                        Authentication auth = new UsernamePasswordAuthenticationToken(member, null,
                                        Collections.emptyList());
                        request = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                        .authentication(auth)
                                        .postProcessRequest(request);
                        com.openforum.domain.context.TenantContext.setTenantId(tenantId);
                        return request;
                };
        }
}
