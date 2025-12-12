package com.openforum.boot;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import com.openforum.infra.jpa.repository.PostJpaRepository;
import com.openforum.infra.jpa.repository.MemberJpaRepository;
import com.openforum.infra.jpa.repository.TenantJpaRepository;
import com.openforum.infra.jpa.repository.CategoryJpaRepository;
import com.openforum.infra.jpa.repository.PostVoteJpaRepository;
import com.openforum.infra.jpa.repository.SubscriptionJpaRepository;
import com.openforum.infra.jpa.repository.PollJpaRepository;
import com.openforum.infra.jpa.repository.PollVoteJpaRepository;
import com.openforum.infra.jpa.repository.TagJpaRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Abstract base class for E2E integration tests.
 * 
 * Provides:
 * - Real PostgreSQL database via Testcontainers
 * - Auto-configured RestAssured with random server port
 * - JWT key generation for authenticated requests
 * - Database cleanup before each test
 */
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    // Singleton container - shared across all E2E test classes
    static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
        postgres.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected ThreadJpaRepository threadJpaRepository;

    @Autowired
    protected PostJpaRepository postJpaRepository;

    @Autowired
    protected MemberJpaRepository memberJpaRepository;

    @Autowired
    protected TenantJpaRepository tenantJpaRepository;

    @Autowired
    protected CategoryJpaRepository categoryJpaRepository;

    @Autowired
    protected PostVoteJpaRepository postVoteJpaRepository;

    @Autowired
    protected SubscriptionJpaRepository subscriptionJpaRepository;

    @Autowired
    protected PollJpaRepository pollJpaRepository;

    @Autowired
    protected PollVoteJpaRepository pollVoteJpaRepository;

    @Autowired
    protected TagJpaRepository tagJpaRepository;

    protected static RSAPrivateKey privateKey;
    protected static RSAPublicKey publicKey;

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
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.public-key", () -> System.getProperty("TEST_PUBLIC_KEY_PATH"));
    }

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    @Autowired
    protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Use JdbcTemplate for native SQL TRUNCATE with CASCADE to handle all FK
        // constraints
        // including self-referencing ones (like posts.reply_to_post_id)
        jdbcTemplate.execute(
                "TRUNCATE TABLE posts, threads, subscriptions, polls, poll_votes, post_votes, categories, tags, members, tenants CASCADE");
    }

    /**
     * Creates a valid JWT token for testing authenticated endpoints.
     */
    protected String createValidToken(String tenantId, String externalId) throws Exception {
        com.nimbusds.jose.JWSSigner signer = new com.nimbusds.jose.crypto.RSASSASigner(privateKey);

        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject(externalId)
                .claim("tenant_id", tenantId)
                .claim("email", externalId + "@test.com")
                .claim("name", "Test User")
                .issuer("openforum-saas")
                .expirationTime(new java.util.Date(System.currentTimeMillis() + 3600000))
                .build();

        com.nimbusds.jwt.SignedJWT signedJWT = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256).build(),
                claims);

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
