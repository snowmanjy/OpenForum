package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.ThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadFactory;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.SecurityConfig;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ThreadController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
        })
@Import({ SecurityConfig.class, MemberJwtAuthenticationConverter.class })
class ThreadControllerTest {
    // Note: @WebMvcTest is a sliced test that only loads web layer
    // We exclude JPA/DataSource auto-configuration and mock all dependencies

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ThreadService threadService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Test
    void createThread_shouldReturnCreated_whenAuthenticated() throws Exception {
        // Given
        String externalId = "ext-123";
        String email = "test@example.com";
        String name = "Test User";
        String token = generateTestToken(externalId, email, name);

        Member member = Member.reconstitute(UUID.randomUUID(), externalId, email, name, false);
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.of(member));

        CreateThreadRequest request = new CreateThreadRequest("Test Thread", "Content");
        Thread thread = ThreadFactory.create("default-tenant", member.getId(), "Test Thread", java.util.Map.of());

        // Mock JwtDecoder to return a valid Jwt
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(externalId)
                .claim("email", email)
                .claim("name", name)
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        when(threadService.createThread(anyString(), any(UUID.class), anyString())).thenReturn(thread);

        // When & Then
        mockMvc.perform(post("/api/v1/threads")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Thread"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createThread_shouldReturnForbidden_whenUnauthenticated() throws Exception {
        CreateThreadRequest request = new CreateThreadRequest("Test Thread", "Content");

        mockMvc.perform(post("/api/v1/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getThread_shouldReturnThread_whenExistsAndAuthenticated() throws Exception {
        // Given
        String externalId = "ext-123";
        String token = generateTestToken(externalId, "test@example.com", "Test User");
        Member member = Member.reconstitute(UUID.randomUUID(), externalId, "test@example.com", "Test User", false);
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.of(member));

        UUID threadId = UUID.randomUUID();
        Thread thread = ThreadFactory.create("tenant-1", member.getId(), "Existing Thread", java.util.Map.of());
        // Reflection to set ID if needed, but Thread.create generates one.
        // We need to mock findById to return this thread.
        // Note: Thread.create generates a random ID. We can't easily set it without
        // reflection or a constructor.
        // For this test, we just use the ID from the created thread object.

        // Mock JwtDecoder
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(externalId)
                .claim("email", "test@example.com")
                .claim("name", "Test User")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        when(threadService.getThread(any(UUID.class))).thenReturn(Optional.of(thread));

        // When & Then
        mockMvc.perform(get("/api/v1/threads/" + thread.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Existing Thread"));
    }

    private String generateTestToken(String sub, String email, String name) {
        String payload = String.format("{\"sub\":\"%s\",\"email\":\"%s\",\"name\":\"%s\"}", sub, email, name);
        return "header." + Base64.getUrlEncoder().encodeToString(payload.getBytes()) + ".signature";
    }
}
