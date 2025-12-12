package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.ThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.factory.ThreadFactory;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.HybridJwtAuthenticationConverter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ThreadController.class)
@Import({ SecurityConfig.class, HybridJwtAuthenticationConverter.class, MemberJwtAuthenticationConverter.class,
                JwtConfig.class })
class ThreadControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ThreadService threadService;

        @MockitoBean
        private MemberRepository memberRepository;

        @MockitoBean
        private com.openforum.rest.service.ThreadQueryService threadQueryService;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey; // Required by HybridJwtAuthenticationConverter

        private Member testMember;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
                UUID memberId = UUID.randomUUID();
                testMember = Member.reconstitute(memberId, "ext-123", "test@example.com", "Test User", false,
                                java.time.Instant.now(), java.time.Instant.now(),
                                com.openforum.domain.valueobject.MemberRole.MEMBER, "test-tenant",
                                null, 0, null, null, null);
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
                com.openforum.domain.context.TenantContext.clear();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        void createThread_shouldReturnCreated_whenAuthenticated() throws Exception {
                // Given
                CreateThreadRequest request = new CreateThreadRequest("Test Thread", "Content", null);
                Thread thread = ThreadFactory.create("default-tenant", testMember.getId(), null, "Test Thread",
                                java.util.Map.of());

                when(threadService.createThread(anyString(), any(UUID.class), anyString(), anyString(), any()))
                                .thenReturn(thread);

                // When & Then
                mockMvc.perform(post("/api/v1/threads")
                                .with(authWithTenant(testMember, "default-tenant"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Test Thread"))
                                .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        void getThread_shouldReturnThread_whenExistsAndAuthenticated() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.rest.service.ThreadQueryService.ThreadQueryResult queryResult = new com.openforum.rest.service.ThreadQueryService.ThreadQueryResult(
                                threadId,
                                "Existing Thread",
                                "OPEN",
                                "OP Content",
                                java.time.Instant.now(),
                                testMember.getId(),
                                "Test User",
                                5);

                when(threadQueryService.getRichThread(any(UUID.class))).thenReturn(Optional.of(queryResult));

                // When & Then
                mockMvc.perform(get("/api/v1/threads/" + threadId)
                                .with(authWithTenant(testMember, "tenant-1")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Existing Thread"))
                                .andExpect(jsonPath("$.content").value("OP Content"));
        }

        @Test
        void getThreads_shouldFilterByMetadata_whenParamsProvided() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.rest.service.ThreadQueryService.ThreadQueryResult queryResult = new com.openforum.rest.service.ThreadQueryService.ThreadQueryResult(
                                threadId,
                                "SAT Question Discussion",
                                "OPEN",
                                "Discussing question 102",
                                java.time.Instant.now(),
                                testMember.getId(),
                                "Test User",
                                3);

                when(threadQueryService.getRichThreads(
                                anyString(),
                                org.mockito.ArgumentMatchers.eq(0),
                                org.mockito.ArgumentMatchers.eq(10),
                                org.mockito.ArgumentMatchers.eq("questionId"),
                                org.mockito.ArgumentMatchers.eq("102")))
                                .thenReturn(java.util.List.of(queryResult));

                // When & Then
                mockMvc.perform(get("/api/v1/threads")
                                .with(authWithTenant(testMember, "tenant-1"))
                                .param("metadataKey", "questionId")
                                .param("metadataValue", "102"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                                .andExpect(jsonPath("$[0].title").value("SAT Question Discussion"));
        }

        private RequestPostProcessor authWithTenant(Member member, String tenantId) {
                return request -> {
                        // First set authentication using Spring Security Test utilities
                        Authentication auth = new UsernamePasswordAuthenticationToken(member, null,
                                        Collections.emptyList());
                        request = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                        .authentication(auth)
                                        .postProcessRequest(request);

                        // Then set tenant context
                        com.openforum.domain.context.TenantContext.setTenantId(tenantId);
                        return request;
                };
        }
}
