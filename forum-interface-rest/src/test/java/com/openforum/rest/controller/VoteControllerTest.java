package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.VoteService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.HybridJwtAuthenticationConverter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoteController.class)
@Import({ SecurityConfig.class, HybridJwtAuthenticationConverter.class, MemberJwtAuthenticationConverter.class,
        JwtConfig.class })
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoteService voteService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private java.security.interfaces.RSAPublicKey publicKey;

    private Member testMember;
    private UUID postId;

    @BeforeEach
    void setUp() {
        testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false,
                java.time.Instant.now(), com.openforum.domain.valueobject.MemberRole.MEMBER, "default-tenant");
        postId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        com.openforum.domain.context.TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId}/vote - upvote returns score delta")
    void vote_upvote_returnsScoreDelta() throws Exception {
        // Given
        when(voteService.vote(eq(postId), eq(testMember.getId()), anyString(), eq(1)))
                .thenReturn(1);

        // When & Then
        mockMvc.perform(put("/api/v1/posts/" + postId + "/vote")
                .with(authWithTenant(testMember, "default-tenant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreDelta").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId}/vote - downvote returns negative score delta")
    void vote_downvote_returnsNegativeScoreDelta() throws Exception {
        // Given
        when(voteService.vote(eq(postId), eq(testMember.getId()), anyString(), eq(-1)))
                .thenReturn(-1);

        // When & Then
        mockMvc.perform(put("/api/v1/posts/" + postId + "/vote")
                .with(authWithTenant(testMember, "default-tenant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": -1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreDelta").value(-1));
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId}/vote - unvote (same value) returns negative delta")
    void vote_unvote_returnsNegativeDelta() throws Exception {
        // Given: voting same value again removes the vote
        when(voteService.vote(eq(postId), eq(testMember.getId()), anyString(), eq(1)))
                .thenReturn(-1); // Removed upvote

        // When & Then
        mockMvc.perform(put("/api/v1/posts/" + postId + "/vote")
                .with(authWithTenant(testMember, "default-tenant"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreDelta").value(-1));
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{postId}/vote - requires authentication")
    void vote_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/posts/" + postId + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authWithTenant(Member member, String tenantId) {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken(member, null, Collections.emptyList());
            request = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .authentication(auth)
                    .postProcessRequest(request);
            com.openforum.domain.context.TenantContext.setTenantId(tenantId);
            return request;
        };
    }
}
