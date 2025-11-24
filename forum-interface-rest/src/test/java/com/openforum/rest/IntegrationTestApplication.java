package com.openforum.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Full integration test application configuration for @SpringBootTest ONLY.
 *
 * IMPORTANT: Uses @SpringBootConfiguration instead of @SpringBootApplication
 * to prevent @WebMvcTest from auto-detecting and loading this configuration.
 *
 * This configuration loads the COMPLETE application context including:
 * - REST controllers (com.openforum.rest)
 * - Application services (com.openforum.application)
 * - JPA infrastructure (com.openforum.infra.jpa)
 *
 * Used by:
 * - JwtAuthenticationIntegrationTest (full Spring Security + JWT + JPA integration)
 *
 * NOT used by:
 * - @WebMvcTest sliced tests (they don't see this configuration)
 *
 * JPA Configuration:
 * - H2 in-memory database with PostgreSQL compatibility mode
 * - Hibernate auto-creates schema (Flyway disabled for tests)
 * - Spring Data JPA repositories enabled
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "com.openforum.rest",           // REST controllers and auth
        "com.openforum.application",    // Application services
        "com.openforum.infra.jpa"       // JPA repositories and adapters
})
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
public class IntegrationTestApplication {
}
