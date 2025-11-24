package com.openforum.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application configuration supporting both WebMvcTest and SpringBootTest.
 *
 * Used by:
 * - @WebMvcTest(ThreadController.class): Only loads web context, excludes JPA
 *   Infrastructure is mocked via @MockBean
 * - @SpringBootTest(classes = TestApplication.class): Loads full context including JPA
 *   Used by integration tests like JwtAuthenticationIntegrationTest
 *
 * Component Scanning:
 * - Scans com.openforum base package to find:
 *   - REST controllers (com.openforum.rest)
 *   - Application services (com.openforum.application)
 *   - JPA adapters (com.openforum.infra.jpa.adapter)
 *
 * JPA Configuration:
 * - @EnableJpaRepositories: Scans for Spring Data JPA repositories
 * - @EntityScan: Scans for JPA entities
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.openforum.rest",           // REST controllers and auth
        "com.openforum.application",    // Application services
        "com.openforum.infra.jpa"       // JPA repositories and adapters
})
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
public class TestApplication {
}
