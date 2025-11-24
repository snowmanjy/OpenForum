package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.IntegrationTestApplication;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for JWT signature verification and JIT provisioning.
 *
 * Tests verify:
 * 1. Valid JWT with correct signature is accepted
 * 2. Invalid JWT with wrong signature is rejected
 * 3. JIT provisioning creates new members automatically
 * 4. Existing members are loaded correctly from database
 * 5. Missing or malformed claims are handled gracefully
 *
 * Architecture: These are full integration tests that test the entire auth flow
 * from HTTP request through Spring Security to domain model.
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Load test keys
        privateKey = loadPrivateKey("src/test/resources/private-key.pem");
        publicKey = loadPublicKey("src/test/resources/public-key.pem");
    }

    @Test
    void shouldAuthenticateWithValidJwt() throws Exception {
        // Given: A valid JWT with signature
        String jwt = createValidJwt("user-123", "john@example.com", "John Doe");

        // When: Making an authenticated request to a real endpoint
        // (Using a non-existent thread ID will return 404, proving auth passed)
        mockMvc.perform(get("/api/v1/threads/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                // Then: Request is authenticated (404 = auth passed, thread not found)
                .andExpect(status().isNotFound());

        // And: Member was created via JIT provisioning
        Member member = memberRepository.findByExternalId("user-123").orElseThrow();
        assertThat(member.getEmail()).isEqualTo("john@example.com");
        assertThat(member.getName()).isEqualTo("John Doe");
        assertThat(member.isBot()).isFalse();
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        // Given: A JWT with invalid signature
        String jwt = createValidJwt("user-456", "jane@example.com", "Jane Doe");
        String tamperedJwt = jwt.substring(0, jwt.length() - 5) + "XXXXX"; // Tamper with signature

        // When/Then: Request is rejected with 401 Unauthorized
        mockMvc.perform(get("/api/v1/threads/test-endpoint")
                        .header("Authorization", "Bearer " + tamperedJwt))
                .andExpect(status().isUnauthorized());

        // And: No member was created
        assertThat(memberRepository.findByExternalId("user-456")).isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        // Given: An expired JWT
        String jwt = createExpiredJwt("user-789", "expired@example.com", "Expired User");

        // When/Then: Request is rejected
        mockMvc.perform(get("/api/v1/threads/test-endpoint")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMissingToken() throws Exception {
        // When/Then: Request without Authorization header is rejected
        mockMvc.perform(get("/api/v1/threads/test-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        // When/Then: Request with malformed JWT is rejected
        mockMvc.perform(get("/api/v1/threads/test-endpoint")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReuseExistingMember() throws Exception {
        // Given: An existing member
        Member existingMember = Member.create("existing-user", "existing@example.com", "Existing User", false);
        memberRepository.save(existingMember);
        UUID existingMemberId = existingMember.getId();

        // When: Authenticating with JWT for same external ID
        String jwt = createValidJwt("existing-user", "updated@example.com", "Updated Name");
        mockMvc.perform(get("/api/v1/threads/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound()); // Passed auth, thread not found

        // Then: Existing member is reused (not duplicated)
        Member reloadedMember = memberRepository.findByExternalId("existing-user").orElseThrow();
        assertThat(reloadedMember.getId()).isEqualTo(existingMemberId);
        // Note: Email/name are not updated - member is immutable in current design
    }

    @Test
    void shouldHandleMissingEmailClaim() throws Exception {
        // Given: JWT without email claim
        String jwt = createJwtWithoutEmail("user-no-email");

        // When: Making request
        mockMvc.perform(get("/api/v1/threads/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound()); // Passed auth, thread not found

        // Then: Member is created with default email
        Member member = memberRepository.findByExternalId("user-no-email").orElseThrow();
        assertThat(member.getEmail()).isEqualTo("unknown@example.com");
    }

    // ========== JWT Creation Helpers ==========

    private String createValidJwt(String sub, String email, String name) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .issuer("test-issuer")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600))) // 1 hour
                .issueTime(new Date())
                .build();

        return signJwt(claimsSet);
    }

    private String createExpiredJwt(String sub, String email, String name) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .expirationTime(Date.from(Instant.now().minusSeconds(3600))) // Expired 1 hour ago
                .issueTime(Date.from(Instant.now().minusSeconds(7200)))
                .build();

        return signJwt(claimsSet);
    }

    private String createJwtWithoutEmail(String sub) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("name", "No Email User")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issueTime(new Date())
                .build();

        return signJwt(claimsSet);
    }

    private String signJwt(JWTClaimsSet claimsSet) throws Exception {
        JWSSigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claimsSet
        );
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    // ========== Key Loading Helpers ==========

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(path)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(path)))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}
