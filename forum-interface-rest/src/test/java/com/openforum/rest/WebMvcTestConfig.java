package com.openforum.rest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal test configuration for @WebMvcTest sliced tests.
 *
 * Only scans REST layer components (controllers, auth, config).
 * Does NOT scan application or infrastructure layers.
 *
 * Used by: ThreadControllerTest
 */
@TestConfiguration
@ComponentScan(basePackages = "com.openforum.rest")
public class WebMvcTestConfig {
}
