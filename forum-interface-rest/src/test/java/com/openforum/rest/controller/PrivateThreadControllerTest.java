package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.dto.CreatePrivatePostRequest;
import com.openforum.application.dto.CreatePrivateThreadRequest;
import com.openforum.application.dto.PrivatePostDto;
import com.openforum.application.dto.PrivateThreadDto;
import com.openforum.application.service.PrivateThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.rest.security.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateThreadController.class)
class PrivateThreadControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PrivateThreadService privateThreadService;

        @MockitoBean
        private com.openforum.domain.repository.MemberRepository memberRepository;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private MockedStatic<SecurityContext> securityContextMock;
        private final UUID currentUserId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                securityContextMock = Mockito.mockStatic(SecurityContext.class);
                securityContextMock.when(SecurityContext::getCurrentUserId).thenReturn(currentUserId);
        }

        @AfterEach
        void tearDown() {
                securityContextMock.close();
        }

        @Test
        @WithMockUser
        void shouldCreatePrivateThread() throws Exception {
                UUID threadId = UUID.randomUUID();
                CreatePrivateThreadRequest request = new CreatePrivateThreadRequest(
                                List.of(UUID.randomUUID()),
                                "Test Subject",
                                "Initial Message");

                when(privateThreadService.createThread(eq("tenant-1"), eq(currentUserId),
                                any(CreatePrivateThreadRequest.class)))
                                .thenReturn(threadId);

                mockMvc.perform(post("/api/v1/private-threads")
                                .header("X-Tenant-ID", "tenant-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location",
                                                "http://localhost/api/v1/private-threads/" + threadId));

                verify(privateThreadService).createThread(eq("tenant-1"), eq(currentUserId),
                                any(CreatePrivateThreadRequest.class));
        }

        @Test
        @WithMockUser
        void shouldCreatePost() throws Exception {
                UUID threadId = UUID.randomUUID();
                CreatePrivatePostRequest request = new CreatePrivatePostRequest("New Reply");

                mockMvc.perform(post("/api/v1/private-threads/{id}/posts", threadId)
                                .header("X-Tenant-ID", "tenant-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk());

                verify(privateThreadService).addPost(eq("tenant-1"), eq(threadId), eq(currentUserId),
                                any(CreatePrivatePostRequest.class));
        }

        @Test
        @WithMockUser
        void shouldGetMyThreads() throws Exception {
                PrivateThreadDto threadDto = new PrivateThreadDto(
                                UUID.randomUUID(),
                                "Subject",
                                List.of(currentUserId),
                                Instant.now(),
                                Instant.now(),
                                Collections.emptyList());

                when(privateThreadService.getMyThreads(eq("tenant-1"), eq(currentUserId), eq(0), eq(20)))
                                .thenReturn(List.of(threadDto));

                mockMvc.perform(get("/api/v1/private-threads")
                                .header("X-Tenant-ID", "tenant-1")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].subject").value("Subject"));
        }
}
