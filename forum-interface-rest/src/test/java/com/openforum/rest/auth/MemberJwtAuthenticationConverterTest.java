package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MemberJwtAuthenticationConverter.
 *
 * Tests the converter in isolation without full Spring Security integration.
 * Verifies JIT provisioning logic and claim extraction.
 */
@ExtendWith(MockitoExtension.class)
class MemberJwtAuthenticationConverterTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MemberJwtAuthenticationConverter(memberRepository);
    }

    @Test
    void shouldConvertJwtWithExistingMember() {
        // Given: An existing member in the repository
        String externalId = "existing-user";
        Member existingMember = Member.create(externalId, "existing@example.com", "Existing User", false);
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.of(existingMember));

        Jwt jwt = createJwt(externalId, "updated@example.com", "Updated Name");

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: Existing member is returned as principal
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(existingMember);
        assertThat(auth.getCredentials()).isNull();

        // And: No new member was created
        verify(memberRepository, times(1)).findByExternalId(externalId);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewMemberViaJitProvisioning() {
        // Given: No existing member
        String externalId = "new-user";
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = createJwt(externalId, "new@example.com", "New User");

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: New member is created and returned as principal
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(Member.class);

        Member member = (Member) auth.getPrincipal();
        assertThat(member.getExternalId()).isEqualTo(externalId);
        assertThat(member.getEmail()).isEqualTo("new@example.com");
        assertThat(member.getName()).isEqualTo("New User");
        assertThat(member.isBot()).isFalse();

        // And: Member was saved
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void shouldHandleMissingEmailClaim() {
        // Given: JWT without email claim
        String externalId = "user-no-email";
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = createJwt(externalId, null, "No Email User");

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: Member is created with default email
        assertThat(auth).isNotNull();
        Member member = (Member) auth.getPrincipal();
        assertThat(member.getEmail()).isEqualTo("unknown@example.com");
        assertThat(member.getName()).isEqualTo("No Email User");
    }

    @Test
    void shouldHandleMissingNameClaim() {
        // Given: JWT without name claim
        String externalId = "user-no-name";
        when(memberRepository.findByExternalId(externalId)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = createJwt(externalId, "noname@example.com", null);

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: Member is created with default name
        assertThat(auth).isNotNull();
        Member member = (Member) auth.getPrincipal();
        assertThat(member.getEmail()).isEqualTo("noname@example.com");
        assertThat(member.getName()).isEqualTo("Unknown User");
    }

    @Test
    void shouldReturnNullForMissingSubject() {
        // Given: JWT without 'sub' claim
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("email", "test@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: Returns null (authentication fails)
        assertThat(auth).isNull();

        // And: No repository interaction
        verify(memberRepository, never()).findByExternalId(anyString());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldReturnNullForBlankSubject() {
        // Given: JWT with blank 'sub' claim
        Jwt jwt = createJwt("   ", "test@example.com", "Test User");

        // When: Converting JWT
        Authentication auth = converter.convert(jwt);

        // Then: Returns null
        assertThat(auth).isNull();

        // And: No repository interaction
        verify(memberRepository, never()).findByExternalId(anyString());
        verify(memberRepository, never()).save(any());
    }

    // ========== Helper Methods ==========

    private Jwt createJwt(String sub, String email, String name) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(sub)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (email != null) {
            builder.claim("email", email);
        }
        if (name != null) {
            builder.claim("name", name);
        }

        return builder.build();
    }
}
