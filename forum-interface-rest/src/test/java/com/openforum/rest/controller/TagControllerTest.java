package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Tag;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.TagRepository;
import com.openforum.rest.auth.JwtAuthenticationFilter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, MemberJwtAuthenticationConverter.class,
        JwtConfig.class })
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private java.security.interfaces.RSAPublicKey publicKey;

    private Member testMember;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testMember = Member.reconstitute(userId, "ext-123", "test@example.com", "Test User", false,
                java.time.Instant.now(), com.openforum.domain.valueobject.MemberRole.MEMBER, "test-tenant");
    }

    @AfterEach
    void tearDown() {
        com.openforum.rest.context.TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void searchTags_shouldReturnMatchingTags_whenAuthenticated() throws Exception {
        // Given
        UUID tagId = UUID.randomUUID();
        Tag tag = Tag.reconstitute(tagId, "default-tenant", "java", 42L);
        when(tagRepository.findByNameStartingWith(anyString(), anyString(), anyInt())).thenReturn(List.of(tag));

        // When & Then
        mockMvc.perform(get("/api/v1/tags/search")
                .param("q", "jav")
                .with(authWithTenant(testMember, "default-tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tagId.toString()))
                .andExpect(jsonPath("$[0].name").value("java"))
                .andExpect(jsonPath("$[0].usageCount").value(42));
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
