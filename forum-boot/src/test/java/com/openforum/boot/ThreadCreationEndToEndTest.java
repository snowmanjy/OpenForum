package com.openforum.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class ThreadCreationEndToEndTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    static Path tempDir;

    private static RSAPrivateKey privateKey;

    @BeforeAll
    static void setupKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

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
    void shouldCreateThreadSuccessfully() throws Exception {
        String tenantId = "test-tenant";
        String externalId = "auth0|55555";
        String email = "creator@test.com";
        String memberName = "Creator";

        // 1. Setup Member
        Member member = Member.create(externalId, email, memberName, false, tenantId);
        memberRepository.save(member);

        // 2. Generate Token
        String token = generateToken(externalId, email);

        // 3. Prepare Request
        CreateThreadRequest request = new CreateThreadRequest(
                "E2E Title",
                "E2E Content");

        // 4. Perform Request with assertions
        mockMvc.perform(post("/api/v1/threads")
                .header("X-Tenant-ID", tenantId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("E2E Title"))
                .andExpect(jsonPath("$.content").value("E2E Content"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.authorName").value(memberName)) // Verify API Aggregation
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.postCount").value(1))
                .andExpect(jsonPath("$.authorId").isNotEmpty());
    }

    private String generateToken(String sub, String email) throws Exception {
        // Use 'openforum-saas' issuer to match SecurityConfig configuration
        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject(sub)
                .issuer("openforum-saas")
                .claim("email", email)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();

        com.nimbusds.jwt.SignedJWT signedJWT = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).keyID("test-key").build(),
                claims);

        com.nimbusds.jose.crypto.RSASSASigner signer = new com.nimbusds.jose.crypto.RSASSASigner(privateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}
