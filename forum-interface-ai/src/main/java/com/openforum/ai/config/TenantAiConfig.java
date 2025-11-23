package com.openforum.ai.config;

import java.util.Map;

public record TenantAiConfig(
        boolean enabled,
        String systemPrompt,
        String staticContext,
        String apiKeyEncrypted) {
    public static TenantAiConfig from(Map<String, Object> tenantConfig) {
        Boolean enabled = (Boolean) tenantConfig.get("ai.enabled");
        String systemPrompt = (String) tenantConfig.get("ai.systemPrompt");
        String staticContext = (String) tenantConfig.get("ai.staticContext");
        String apiKey = (String) tenantConfig.get("ai.apiKey");

        return new TenantAiConfig(
                enabled != null && enabled,
                systemPrompt != null ? systemPrompt : "You are a helpful assistant.",
                staticContext != null ? staticContext : "",
                apiKey != null ? apiKey : "");
    }
}
