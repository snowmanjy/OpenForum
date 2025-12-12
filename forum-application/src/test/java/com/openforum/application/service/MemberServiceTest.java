package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.valueobject.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository);
    }

    @Test
    void shouldCreateNewMemberWhenNotExists() {
        // Given
        String externalId = "ext-1";
        String email = "test@example.com";
        String name = "Test User";
        String role = "ADMIN";
        String tenantId = "tenant-1";

        when(memberRepository.findByExternalId(tenantId, externalId)).thenReturn(Optional.empty());

        // When
        memberService.upsertMember(externalId, email, name, role, tenantId, null);

        // Then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member savedMember = captor.getValue();
        assertThat(savedMember.getExternalId()).isEqualTo(externalId);
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getName()).isEqualTo(name);
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void shouldUpdateExistingMemberRoleAndDetails() {
        // Given
        String externalId = "ext-1";
        String email = "new@example.com";
        String name = "New Name";
        String role = "MODERATOR";
        String tenantId = "tenant-1";

        Member existingMember = Member.createWithRole(externalId, "old@example.com", "Old Name", false,
                MemberRole.MEMBER, tenantId);
        when(memberRepository.findByExternalId(tenantId, externalId)).thenReturn(Optional.of(existingMember));

        // When
        memberService.upsertMember(externalId, email, name, role, tenantId, null);

        // Then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member savedMember = captor.getValue();
        assertThat(savedMember.getExternalId()).isEqualTo(externalId);
        assertThat(savedMember.getEmail()).isEqualTo(email);
        assertThat(savedMember.getName()).isEqualTo(name);
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.MODERATOR);
    }

    @Test
    void shouldThrowExceptionWhenRoleIsInvalid() {
        // Given
        String externalId = "ext-1";
        String email = "test@example.com";
        String name = "Test User";
        String invalidRole = "INVALID_ROLE";
        String tenantId = "tenant-1";

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            memberService.upsertMember(externalId, email, name, invalidRole, tenantId, null);
        });
    }
}
