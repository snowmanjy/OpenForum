package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.context.TenantContext;
import com.openforum.domain.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridJwtAuthenticationConverterTest {

    @Mock
    private MemberRepository memberRepository;

    private HybridJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        converter = new HybridJwtAuthenticationConverter(memberRepository);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void convert_shouldUseTenantIdFromJwt_whenPresent() {
        // Given
        String expectedTenantId = "jwt-tenant";
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("tenant_id")).thenReturn(expectedTenantId);

        when(memberRepository.findByExternalId(eq(expectedTenantId), anyString()))
                .thenReturn(Optional.of(Mockito.mock(Member.class)));

        // When
        converter.convert(jwt);

        // Then
        Assertions.assertEquals(expectedTenantId, TenantContext.getTenantId());
    }

    @Test
    void convert_shouldUseExistingTenantContext_whenJwtHasNoTenantId_andContextIsSet() {
        // Given
        String expectedTenantId = "header-tenant";
        TenantContext.setTenantId(expectedTenantId); // Simulate Filter setting it

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("tenant_id")).thenReturn(null);

        when(memberRepository.findByExternalId(eq(expectedTenantId), anyString()))
                .thenReturn(Optional.of(Mockito.mock(Member.class)));

        // When
        converter.convert(jwt);

        // Then
        Assertions.assertEquals(expectedTenantId, TenantContext.getTenantId());
    }

    @Test
    void convert_shouldThrowException_whenJwtHasNoTenantId_andContextIsEmpty() {
        // Given
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("tenant_id")).thenReturn(null);

        // When & Then
        Assertions.assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            converter.convert(jwt);
        });
    }

    @Test
    void convert_shouldThrowException_whenJwtHasNoEmail_orName() {
        // Given
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-1");
        when(jwt.getClaimAsString("tenant_id")).thenReturn("tenant-1");
        // Name and Email are null by default mock

        // When & Then
        Assertions.assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            converter.convert(jwt);
        });
    }
}
