package com.openforum.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.application.dto.CreatePollRequest;
import com.openforum.application.dto.PollDto;
import com.openforum.application.dto.VotePollRequest;
import com.openforum.application.service.PollService;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.security.SecurityContext;
import com.openforum.rest.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PollController.class)
class PollControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PollService pollService;

        @MockitoBean
        private MemberRepository memberRepository;

        @MockitoBean
        private java.security.interfaces.RSAPublicKey publicKey;

        private MockedStatic<SecurityContext> securityContextMock;
        private final UUID currentUserId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                securityContextMock = mockStatic(SecurityContext.class);
                securityContextMock.when(SecurityContext::getCurrentUserId).thenReturn(currentUserId);
                TenantContext.setTenantId("tenant-1");
        }

        @AfterEach
        void tearDown() {
                securityContextMock.close();
                TenantContext.clear();
        }

        @Test
        @WithMockUser
        void shouldCreatePoll() throws Exception {
                UUID postId = UUID.randomUUID();
                UUID pollId = UUID.randomUUID();
                CreatePollRequest request = new CreatePollRequest("Question?", List.of("A",
                                "B"),
                                Instant.now().plusSeconds(3600), false);

                when(pollService.createPoll(eq("tenant-1"), eq(postId),
                                any(CreatePollRequest.class))).thenReturn(pollId);

                mockMvc.perform(post("/api/v1/posts/{postId}/polls", postId)
                                .header("X-Tenant-ID", "tenant-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/api/v1/polls/" + pollId));
        }

        @Test
        @WithMockUser
        void shouldCastVote() throws Exception {
                UUID pollId = UUID.randomUUID();
                VotePollRequest request = new VotePollRequest(0);

                mockMvc.perform(post("/api/v1/polls/{pollId}/votes", pollId)
                                .header("X-Tenant-ID", "tenant-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isOk());

                verify(pollService).castVote(eq("tenant-1"), eq(pollId), eq(currentUserId),
                                any(VotePollRequest.class));
        }

        @Test
        @WithMockUser
        void shouldGetPoll() throws Exception {
                UUID pollId = UUID.randomUUID();
                PollDto pollDto = new PollDto(
                                pollId,
                                UUID.randomUUID(),
                                "Question?",
                                List.of("A", "B"),
                                Instant.now().plusSeconds(3600),
                                false,
                                Instant.now(),
                                List.of(1, 0),
                                true,
                                List.of(0));

                when(pollService.getPoll(eq("tenant-1"), eq(pollId),
                                eq(currentUserId))).thenReturn(pollDto);

                mockMvc.perform(get("/api/v1/polls/{pollId}", pollId)
                                .header("X-Tenant-ID", "tenant-1")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(pollId.toString()))
                                .andExpect(jsonPath("$.question").value("Question?"))
                                .andExpect(jsonPath("$.options[0]").value("A"))
                                .andExpect(jsonPath("$.voteCounts[0]").value(1))
                                .andExpect(jsonPath("$.hasVoted").value(true));
        }
}
