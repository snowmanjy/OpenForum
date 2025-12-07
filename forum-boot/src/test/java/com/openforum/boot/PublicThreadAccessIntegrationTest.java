package com.openforum.boot;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class PublicThreadAccessIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private MemberRepository memberRepository;

    @TempDir
    static Path tempDir;

    // We need to mock the JWT key setup because the application context requires it
    // to start,
    // even though we are testing public access.
    @BeforeAll
    static void setupKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

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
    void shouldAccessThreadPublicly_WithoutJwtToken() throws Exception {
        // Given
        String tenantId = "public-tenant";

        // Create Author (Member)
        Member author = Member.create(
                "auth0|123456",
                "author@test.com",
                "Test Author",
                false,
                tenantId);
        memberRepository.save(author);

        // Create Thread
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .authorId(author.getId())
                .title("Public Thread Title")
                .isNew(true)
                .build();
        threadRepository.save(thread);

        // When & Then
        mockMvc.perform(get("/api/v1/threads/" + thread.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(thread.getId().toString()))
                .andExpect(jsonPath("$.title").value("Public Thread Title"));
    }
}
