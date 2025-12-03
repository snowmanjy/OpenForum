package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.service.CategoryService;
import com.openforum.domain.aggregate.Category;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.auth.JwtAuthenticationFilter;
import com.openforum.rest.auth.MemberJwtAuthenticationConverter;
import com.openforum.rest.config.JwtConfig;
import com.openforum.rest.config.SecurityConfig;
import com.openforum.rest.controller.CategoryController.CreateCategoryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, MemberJwtAuthenticationConverter.class,
                JwtConfig.class })
class CategoryControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CategoryService categoryService;

        @MockitoBean
        private MemberRepository memberRepository;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private Member testMember;

        @BeforeEach
        void setUp() {
                testMember = Member.reconstitute(UUID.randomUUID(), "ext-123", "test@example.com", "Test User", false,
                                java.time.Instant.now(), com.openforum.domain.valueobject.MemberRole.MEMBER,
                                "default-tenant");
        }

        @AfterEach
        void tearDown() {
                com.openforum.rest.context.TenantContext.clear();
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        void getCategories_shouldReturnList_whenAuthenticated() throws Exception {
                // Given
                UUID categoryId = UUID.randomUUID();
                Category category = Category.reconstitute(categoryId, "default-tenant", "General", "general",
                                "General discussions", false);
                when(categoryService.getCategories(anyString())).thenReturn(List.of(category));

                // When & Then
                mockMvc.perform(get("/api/v1/categories")
                                .with(authWithTenant(testMember, "default-tenant")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                                .andExpect(jsonPath("$[0].name").value("General"))
                                .andExpect(jsonPath("$[0].slug").value("general"));
        }

        @Test
        void createCategory_shouldReturnCreated_whenAuthenticated() throws Exception {
                // Given
                UUID categoryId = UUID.randomUUID();
                CreateCategoryRequest request = new CreateCategoryRequest("News", "news", "News and announcements",
                                false);
                Category category = Category.reconstitute(categoryId, "default-tenant", "News", "news",
                                "News and announcements", false);

                when(categoryService.createCategory(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                                .thenReturn(category);

                // When & Then
                mockMvc.perform(post("/api/v1/categories")
                                .with(authWithTenant(testMember, "default-tenant"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                                .andExpect(jsonPath("$.name").value("News"))
                                .andExpect(jsonPath("$.slug").value("news"));
        }

        private RequestPostProcessor authWithTenant(Member member, String tenantId) {
                return request -> {
                        Authentication auth = new UsernamePasswordAuthenticationToken(member, null,
                                        Collections.emptyList());
                        request = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                        .authentication(auth)
                                        .postProcessRequest(request);
                        com.openforum.rest.context.TenantContext.setTenantId(tenantId);
                        return request;
                };
        }
}
