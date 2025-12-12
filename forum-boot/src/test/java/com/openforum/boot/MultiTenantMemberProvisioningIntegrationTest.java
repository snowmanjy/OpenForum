package com.openforum.boot;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class MultiTenantMemberProvisioningIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setupKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        // Write public key to temp file in PEM format
        String pem = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(publicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";

        File publicKeyFile = tempDir.resolve("public-key.pem").toFile();
        Files.writeString(publicKeyFile.toPath(), pem);

        System.setProperty("TEST_PUBLIC_KEY_PATH", "file:" + publicKeyFile.getAbsolutePath());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.public-key", () -> System.getProperty("TEST_PUBLIC_KEY_PATH"));
    }

    @Test
    void shouldProvisionMemberInMultipleTenants_WithSameExternalId() throws Exception {
        String externalId = "user-" + UUID.randomUUID();
        String tenant1 = "tenant-A";
        String tenant2 = "tenant-B";

        // 1. Create Member in Tenant A
        String token1 = createValidToken(tenant1, externalId);
        mockMvc.perform(post("/api/v1/tenants")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"id\": \"t1\", \"name\": \"T1\", \"slug\": \"t1\", \"externalOwnerId\": \"owner1\", \"ownerEmail\": \"owner1@test.com\", \"ownerName\": \"Owner One\", \"config\": {}}"))
                .andExpect(status().isOk());

        // Verify member created in Tenant A
        Optional<Member> memberA = memberRepository.findByExternalId(tenant1, externalId);
        assertThat(memberA).isPresent();
        assertThat(memberA.get().getTenantId()).isEqualTo(tenant1);

        // 2. Create Member in Tenant B (Same External ID)
        String token2 = createValidToken(tenant2, externalId);
        mockMvc.perform(post("/api/v1/tenants")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"id\": \"t2\", \"name\": \"T2\", \"slug\": \"t2\", \"externalOwnerId\": \"owner2\", \"ownerEmail\": \"owner2@test.com\", \"ownerName\": \"Owner Two\", \"config\": {}}"))
                .andExpect(status().isOk());
        // If race condition/constraint failed, we might see 500 or 403
        // without member creation.

        // Verify member created in Tenant B
        Optional<Member> memberB = memberRepository.findByExternalId(tenant2, externalId);
        assertThat(memberB).isPresent();
        assertThat(memberB.get().getTenantId()).isEqualTo(tenant2);

        // Verify they are different records
        assertThat(memberA.get().getId()).isNotEqualTo(memberB.get().getId());
    }

    private String createValidToken(String tenantId, String memberId) throws Exception {
        JWSSigner signer = new RSASSASigner(privateKey);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(memberId)
                .claim("tenant_id", tenantId)
                .claim("email", memberId + "@test.com")
                .claim("name", "Test User")
                .issuer("openforum-saas")
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claims);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
