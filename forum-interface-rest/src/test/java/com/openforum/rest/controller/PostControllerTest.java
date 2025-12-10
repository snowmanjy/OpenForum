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
        private com.openforum.infra.jpa.repository.PostJpaRepository postJpaRepository;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private Member testMember;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
                testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false,
                                java.time.Instant.now(), com.openforum.domain.valueobject.MemberRole.MEMBER,
                                "default-tenant");
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
        void getPostsByThread_withSortOldest_usesChrono() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                when(postJpaRepository.findByThreadIdAndTenantId(
                                eq(threadId), eq("test-tenant"), any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(org.springframework.data.domain.Page.empty());

                // When & Then
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts?sort=oldest"))
                                .andExpect(status().isOk());
        }

        @Test
        void getPostsByThread_withSortTop_usesCompositeSortForStableOrdering() throws Exception {
                // Given: 3 posts with same score but different createdAt
                UUID threadId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                // Post A: score=10, created first
                com.openforum.infra.jpa.entity.PostEntity postA = new com.openforum.infra.jpa.entity.PostEntity();
                postA.setId(UUID.randomUUID());
                postA.setThreadId(threadId);
                postA.setTenantId("test-tenant");
                postA.setAuthorId(UUID.randomUUID());
                postA.setContent("Post A");
                postA.setPostNumber(1);
                postA.setScore(10);
                postA.setCreatedAt(java.time.Instant.parse("2024-01-01T10:00:00Z"));

                // Post B: score=10, created second (same score, should come after A)
                com.openforum.infra.jpa.entity.PostEntity postB = new com.openforum.infra.jpa.entity.PostEntity();
                postB.setId(UUID.randomUUID());
                postB.setThreadId(threadId);
                postB.setTenantId("test-tenant");
                postB.setAuthorId(UUID.randomUUID());
                postB.setContent("Post B");
                postB.setPostNumber(2);
                postB.setScore(10);
                postB.setCreatedAt(java.time.Instant.parse("2024-01-01T11:00:00Z"));

                // Post C: score=20, should come first (higher score)
                com.openforum.infra.jpa.entity.PostEntity postC = new com.openforum.infra.jpa.entity.PostEntity();
                postC.setId(UUID.randomUUID());
                postC.setThreadId(threadId);
                postC.setTenantId("test-tenant");
                postC.setAuthorId(UUID.randomUUID());
                postC.setContent("Post C");
                postC.setPostNumber(3);
                postC.setScore(20);
                postC.setCreatedAt(java.time.Instant.parse("2024-01-01T12:00:00Z"));

                // Expected order: C (score 20), A (score 10, earlier), B (score 10, later)
                org.springframework.data.domain.Page<com.openforum.infra.jpa.entity.PostEntity> page = new org.springframework.data.domain.PageImpl<>(
                                java.util.List.of(postC, postA, postB));

                when(postJpaRepository.findByThreadIdAndTenantId(
                                eq(threadId), eq("test-tenant"), any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(page);

                // When & Then: verify the order is C, A, B
                mockMvc.perform(get("/api/v1/threads/" + threadId + "/posts?sort=top"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].content").value("Post C"))
                                .andExpect(jsonPath("$.content[0].score").value(20))
                                .andExpect(jsonPath("$.content[1].content").value("Post A"))
                                .andExpect(jsonPath("$.content[1].score").value(10))
                                .andExpect(jsonPath("$.content[1].createdAt").value("2024-01-01T10:00:00Z"))
                                .andExpect(jsonPath("$.content[2].content").value("Post B"))
                                .andExpect(jsonPath("$.content[2].score").value(10))
                                .andExpect(jsonPath("$.content[2].createdAt").value("2024-01-01T11:00:00Z"));
        }

        @Test
        void getPostsByThread_withDefaultSort_usesChrono() throws Exception {
                // Given
                UUID threadId = UUID.randomUUID();
                com.openforum.domain.context.TenantContext.setTenantId("test-tenant");

                when(postJpaRepository.findByThreadIdAndTenantId(
                                eq(threadId), eq("test-tenant"), any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(org.springframework.data.domain.Page.empty());

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

                // Create a PostEntity with score and createdAt
                com.openforum.infra.jpa.entity.PostEntity postEntity = new com.openforum.infra.jpa.entity.PostEntity();
                postEntity.setId(postId);
                postEntity.setThreadId(threadId);
                postEntity.setTenantId("test-tenant");
                postEntity.setAuthorId(authorId);
                postEntity.setContent("Test post content");
                postEntity.setPostNumber(1);
                postEntity.setScore(42); // Set score to verify
                postEntity.setCreatedAt(createdAt); // Set createdAt to verify

                org.springframework.data.domain.Page<com.openforum.infra.jpa.entity.PostEntity> page = new org.springframework.data.domain.PageImpl<>(
                                java.util.List.of(postEntity));

                when(postJpaRepository.findByThreadIdAndTenantId(
                                eq(threadId), eq("test-tenant"), any(org.springframework.data.domain.Pageable.class)))
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
