package com.openforum.ai.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAiConfigTest {

    @Test
    void shouldParseFullConfig() {
        // Given
        Map<String, Object> tenantConfig = new HashMap<>();
        tenantConfig.put("ai.enabled", true);
        tenantConfig.put("ai.systemPrompt", "You are an expert assistant.");
        tenantConfig.put("ai.staticContext", "Product documentation here.");
        tenantConfig.put("ai.apiKey", "encrypted_key_123");

        // When
        TenantAiConfig config = TenantAiConfig.from(tenantConfig);

        // Then
        assertThat(config.enabled()).isTrue();
        assertThat(config.systemPrompt()).isEqualTo("You are an expert assistant.");
        assertThat(config.staticContext()).isEqualTo("Product documentation here.");
        assertThat(config.apiKeyEncrypted()).isEqualTo("encrypted_key_123");
    }

    @Test
    void shouldUseDefaultsForMissingFields() {
        // Given
        Map<String, Object> tenantConfig = new HashMap<>();

        // When
        TenantAiConfig config = TenantAiConfig.from(tenantConfig);

        // Then
        assertThat(config.enabled()).isFalse();
        assertThat(config.systemPrompt()).contains("helpful assistant");
        assertThat(config.staticContext()).isEmpty();
        assertThat(config.apiKeyEncrypted()).isEmpty();
    }

    @Test
    void shouldDisableWhenEnabledIsFalse() {
        // Given
        Map<String, Object> tenantConfig = new HashMap<>();
        tenantConfig.put("ai.enabled", false);

        // When
        TenantAiConfig config = TenantAiConfig.from(tenantConfig);

        // Then
        assertThat(config.enabled()).isFalse();
    }

    @Test
    void shouldHandlePartialConfig() {
        // Given
        Map<String, Object> tenantConfig = new HashMap<>();
        tenantConfig.put("ai.enabled", true);
        tenantConfig.put("ai.apiKey", "key123");
        // systemPrompt and staticContext missing

        // When
        TenantAiConfig config = TenantAiConfig.from(tenantConfig);

        // Then
        assertThat(config.enabled()).isTrue();
        assertThat(config.apiKeyEncrypted()).isEqualTo("key123");
        assertThat(config.systemPrompt()).contains("helpful assistant");
        assertThat(config.staticContext()).isEmpty();
    }
}
