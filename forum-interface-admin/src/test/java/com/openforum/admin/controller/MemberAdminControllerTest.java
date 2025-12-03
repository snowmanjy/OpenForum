package com.openforum.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.admin.dto.UpsertMemberRequest;
import com.openforum.application.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberAdminController.class)
class MemberAdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private MemberService memberService;

        @MockitoBean
        private com.openforum.admin.service.BulkImportService bulkImportService;

        @MockitoBean
        private com.openforum.admin.service.BulkMemberImportService bulkMemberImportService;

        @Test
        void shouldUpsertMember() throws Exception {
                // Given
                UpsertMemberRequest request = new UpsertMemberRequest(
                                "ext-1",
                                "test@example.com",
                                "Test User",
                                "ADMIN",
                                "tenant-1");

                // When
                mockMvc.perform(put("/admin/v1/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Then
                verify(memberService).upsertMember(
                                "ext-1",
                                "test@example.com",
                                "Test User",
                                "ADMIN",
                                "tenant-1");
        }
}
