package com.openforum.application.service;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void getTenant_shouldReturnTenant_whenExists() {
        // Given
        String tenantId = "tenant-1";
        Tenant tenant = com.openforum.domain.factory.TenantFactory.create(tenantId, "slug-1", "Tenant 1", Map.of());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // When
        Optional<Tenant> result = tenantService.getTenant(tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(tenantId);
    }

    @Test
    void updateTenantConfig_shouldUpdate_whenTenantExists() {
        // Given
        String tenantId = "tenant-1";
        Map<String, Object> oldConfig = Map.of("key", "old");
        Map<String, Object> newConfig = Map.of("key", "new");

        Tenant existingTenant = com.openforum.domain.factory.TenantFactory.create(tenantId, "slug-1", "Tenant 1",
                oldConfig);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existingTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Tenant updatedTenant = tenantService.updateTenantConfig(tenantId, newConfig);

        // Then
        assertThat(updatedTenant.getConfig()).containsEntry("key", "new");
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void updateTenantConfig_shouldThrow_whenTenantDoesNotExist() {
        // Given
        String tenantId = "tenant-1";
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantService.updateTenantConfig(tenantId, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");
    }
}
