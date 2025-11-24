package com.openforum.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
 * JPA annotations are included because:
 * - @WebMvcTest explicitly specifies which controller(s) to load, so it ignores these annotations
 * - @SpringBootTest loads everything and needs JPA for database tests
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.openforum")
@EntityScan(basePackages = "com.openforum")
public class TestApplication {
}
