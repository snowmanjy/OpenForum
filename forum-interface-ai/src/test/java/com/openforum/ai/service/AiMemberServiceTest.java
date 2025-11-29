package com.openforum.ai.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AiMemberService aiMemberService;

    private final String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        aiMemberService = new AiMemberService(memberRepository);
    }

    @Test
    void shouldReturnExistingAiMember() {
        // Given
        Member existingMember = Member.create("ai-assistant", "ai@forum.local", "AI Assistant", true);
        when(memberRepository.findByExternalId(eq(tenantId), anyString()))
                .thenReturn(Optional.of(existingMember));

        // When
        Member result = aiMemberService.getOrCreateAiMember(tenantId);

        // Then
        assertThat(result).isEqualTo(existingMember);
        assertThat(result.isBot()).isTrue();
        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewAiMemberWhenNotExists() {
        // Given
        when(memberRepository.findByExternalId(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Member result = aiMemberService.getOrCreateAiMember(tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isBot()).isTrue();
        assertThat(result.getName()).isEqualTo("AI Assistant");
        assertThat(result.getEmail()).isEqualTo("ai@openforum.com");
        verify(memberRepository).save(any(Member.class));
    }
}
