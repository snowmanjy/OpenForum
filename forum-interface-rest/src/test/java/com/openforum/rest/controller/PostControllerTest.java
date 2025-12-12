package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.HybridJwtAuthenticationConverter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import com.openforum.rest.controller.dto.CreatePostRequest;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import({ SecurityConfig.class, HybridJwtAuthenticationConverter.class, MemberJwtAuthenticationConverter.class,
                JwtConfig.class })
class PostControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PostService postService;

        @MockitoBean
        private MemberRepository memberRepository;

        @MockitoBean
        private com.openforum.rest.service.PostQueryService postQueryService;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private Member testMember;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
                testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false,
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
        void createPost_shouldReturnCreated_whenAuthenticated() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                CreatePostRequest request = new CreatePostRequest("Test Content", null, null, null);
                UUID postId = UUID.randomUUID();
                Post post = Post.builder()
                                .id(postId)
                                .threadId(threadId)
                                .tenantId("test-tenant")
                                .authorId(testMember.getId())
                                .content("Test Content")
                                .isNew(true)
                                .build();

                when(postService.createReply(eq(threadId), eq(testMember.getId()), anyString(), anyString(), any(),
                                any(),
                                any()))
                                .thenReturn(post);

                // When & Then
                mockMvc.perform(post("/api/v1/threads/" + threadId + "/posts")
                                .with(authWithTenant(testMember, "default-tenant"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(postId.toString()));
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

        @Test
        void getPostsByThread_withSortOldest_shouldReturnPosts() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                com.openforum.rest.service.PostQueryService.PostQueryResult result = new com.openforum.rest.service.PostQueryService.PostQueryResult(
                                UUID.randomUUID(),
                                threadId,
                                authorId,
                                "Test Author",
                                "Test Content",
                                null,
                                1,
                                0,
                                java.time.Instant.now(),
                                false);

                com.openforum.rest.service.PostQueryService.PostQueryPage page = new com.openforum.rest.service.PostQueryService.PostQueryPage(
                                java.util.List.of(result), 0, 20, 1, 1, true, true);

                when(postQueryService.getPostsByThread(eq(threadId), eq("test-tenant"), anyInt(), anyInt(),
                                anyString()))
                                .thenReturn(page);

                // When & Then
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts?sort=oldest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getPostsByThread_withSortTop_shouldReturnPosts() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                com.openforum.rest.service.PostQueryService.PostQueryPage page = new com.openforum.rest.service.PostQueryService.PostQueryPage(
                                java.util.List.of(), 0, 20, 0, 0, true, true);

                when(postQueryService.getPostsByThread(eq(threadId), eq("test-tenant"), anyInt(), anyInt(), eq("top")))
                                .thenReturn(page);

                // When & Then
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts?sort=top"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getPostsByThread_withDefaultSort_shouldReturnPosts() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                com.openforum.rest.service.PostQueryService.PostQueryPage page = new com.openforum.rest.service.PostQueryService.PostQueryPage(
                                java.util.List.of(), 0, 20, 0, 0, true, true);

                when(postQueryService.getPostsByThread(eq(threadId), eq("test-tenant"), anyInt(), anyInt(),
                                eq("oldest")))
                                .thenReturn(page);

                // When & Then - no sort param defaults to oldest
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts"))
                                .andExpect(status().isOk());
        }

        @Test
        void getPostsByThread_shouldReturnScoreAndCreatedAt() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                java.time.Instant createdAt = java.time.Instant.parse("2024-06-15T10:30:00Z");

                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                com.openforum.rest.service.PostQueryService.PostQueryResult result = new com.openforum.rest.service.PostQueryService.PostQueryResult(
                                postId, threadId, authorId, "Test Author",
                                "Test post content", null, 1, 42, createdAt, false);

                com.openforum.rest.service.PostQueryService.PostQueryPage page = new com.openforum.rest.service.PostQueryService.PostQueryPage(
                                java.util.List.of(result), 0, 20, 1, 1, true, true);

                when(postQueryService.getPostsByThread(eq(threadId), eq("test-tenant"), anyInt(), anyInt(),
                                anyString()))
                                .thenReturn(page);

                // When & Then - verify score and createdAt are correctly mapped
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(postId.toString()))
                                .andExpect(jsonPath("$.content[0].score").value(42))
                                .andExpect(jsonPath("$.content[0].createdAt").value("2024-06-15T10:30:00Z"))
                                .andExpect(jsonPath("$.content[0].postNumber").value(1));
        }
}
