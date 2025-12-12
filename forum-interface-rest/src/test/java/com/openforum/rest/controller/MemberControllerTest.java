package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.context.TenantContext;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.valueobject.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberControllerTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberController memberController;
    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        memberController = new MemberController(memberRepository);
        tenantContextMock = Mockito.mockStatic(TenantContext.class);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void getMemberByExternalId_Found() {
        String tenantId = "tenant-1";
        String externalId = "ext-123";
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(tenantId);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(UUID.randomUUID());
        when(member.getExternalId()).thenReturn(externalId);
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(member.getJoinedAt()).thenReturn(Instant.now());

        when(memberRepository.findByExternalId(tenantId, externalId)).thenReturn(Optional.of(member));

        ResponseEntity<MemberController.MemberDto> response = memberController.getMemberByExternalId(externalId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(externalId, response.getBody().externalId());
    }

    @Test
    void getMemberByExternalId_NotFound() {
        String tenantId = "tenant-1";
        String externalId = "ext-123";
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(tenantId);

        when(memberRepository.findByExternalId(tenantId, externalId)).thenReturn(Optional.empty());

        ResponseEntity<MemberController.MemberDto> response = memberController.getMemberByExternalId(externalId);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getMemberById_Found_CorrectTenant() {
        String tenantId = "tenant-1";
        UUID id = UUID.randomUUID();
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(tenantId);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getTenantId()).thenReturn(tenantId);
        when(member.getRole()).thenReturn(MemberRole.MEMBER);
        when(member.getJoinedAt()).thenReturn(Instant.now());

        when(memberRepository.findById(id)).thenReturn(Optional.of(member));

        ResponseEntity<MemberController.MemberDto> response = memberController.getMemberById(id);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().id());
    }

    @Test
    void getMemberById_Found_WrongTenant_ShouldReturn404() {
        String tenantId = "tenant-1";
        String otherTenantId = "tenant-2";
        UUID id = UUID.randomUUID();
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(tenantId);

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getTenantId()).thenReturn(otherTenantId); // DIFFERENT TENANT

        when(memberRepository.findById(id)).thenReturn(Optional.of(member));

        ResponseEntity<MemberController.MemberDto> response = memberController.getMemberById(id);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getMemberById_NotFound() {
        String tenantId = "tenant-1";
        UUID id = UUID.randomUUID();
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(tenantId);

        when(memberRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<MemberController.MemberDto> response = memberController.getMemberById(id);

        assertEquals(404, response.getStatusCode().value());
    }
}
