package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.JwtAuthenticationFilter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, MemberJwtAuthenticationConverter.class,
        JwtConfig.class })
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private java.security.interfaces.RSAPublicKey publicKey;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false);
    }

    @AfterEach
    void tearDown() {
        com.openforum.rest.context.TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void searchUsers_shouldReturnMatchingUsers_whenAuthenticated() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Member searchResult = Member.reconstitute(userId, "ext-456", "john@example.com", "John Doe", false);
        when(memberRepository.searchByHandleOrName(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(searchResult));

        // When & Then
        mockMvc.perform(get("/api/v1/users/search")
                .param("q", "john")
                .with(authWithTenant(testMember, "default-tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));
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
