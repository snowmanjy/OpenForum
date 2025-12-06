package com.openforum.rest.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate RSA key pair for testing
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        filter = new JwtAuthenticationFilter(memberRepository, publicKey);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidJwt() throws Exception {
        // Given
        String tenantId = "tenant-1";
        String userId = "user-123";
        Member member = Member.create("ext-123", "test@example.com", "Test User", false, "test-tenant");

        when(memberRepository.findByExternalId(tenantId, userId))
                .thenReturn(Optional.of(member));

        String token = createValidToken(tenantId, userId);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // Context should be cleared after filter execution
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void shouldContinueWithoutAuthentication_whenNoAuthHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueWithoutAuthentication_whenInvalidToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldClearContext_afterProcessing() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // Manually set contexts to verify they're cleared
        TenantContext.setTenantId("test-tenant");
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user", null));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleRaceConditionDuringMemberCreation() throws Exception {
        // Given
        String tenantId = "tenant-1";
        String userId = "user-race-condition";
        Member existingMember = Member.create(userId, "test@example.com", "Test User", false, tenantId);

        // First find returns empty (simulating new user)
        // Second find returns member (simulating created by another thread)
        when(memberRepository.findByExternalId(tenantId, userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingMember));

        // Save throws DataIntegrityViolationException (simulating race condition)
        when(memberRepository.save(any(Member.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        String token = createValidToken(tenantId, userId);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // Capture authentication inside the filter chain
        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(existingMember);
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(memberRepository, times(2)).findByExternalId(tenantId, userId);
        verify(memberRepository).save(any(Member.class));
        verify(filterChain).doFilter(request, response);
    }

    private String createValidToken(String tenantId, String userId) throws Exception {
        JWSSigner signer = new RSASSASigner(privateKey);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("tenant_id", tenantId)
                .issuer("saas-control-plane")
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claims);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
