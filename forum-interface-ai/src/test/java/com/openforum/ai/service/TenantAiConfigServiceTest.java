package com.openforum.ai.service;

import com.openforum.ai.config.TenantAiConfig;
import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.factory.TenantFactory;
import com.openforum.domain.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAiConfigServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    private TenantAiConfigService service;

    @BeforeEach
    void setUp() {
        service = new TenantAiConfigService(tenantRepository);
    }

    @Test
    void shouldReturnConfigWhenTenantExists() {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("ai.enabled", true);
        config.put("ai.systemPrompt", "Be helpful");
        config.put("ai.apiKey", "key123");

        Tenant tenant = TenantFactory.create("tenant123", "slug-123", "Tenant 123", config);

        when(tenantRepository.findById("tenant123")).thenReturn(Optional.of(tenant));

        // When
        Optional<TenantAiConfig> result = service.getConfig("tenant123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().enabled()).isTrue();
        assertThat(result.get().systemPrompt()).isEqualTo("Be helpful");
        assertThat(result.get().apiKeyEncrypted()).isEqualTo("key123");
    }

    @Test
    void shouldReturnEmptyWhenTenantNotFound() {
        // Given
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<TenantAiConfig> result = service.getConfig("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }
}
