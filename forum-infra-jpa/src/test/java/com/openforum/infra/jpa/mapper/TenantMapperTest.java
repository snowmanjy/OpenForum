package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.infra.jpa.entity.TenantEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private final TenantMapper mapper = new TenantMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        String tenantId = "tenant-1";
        Map<String, Object> config = Map.of("theme", "dark", "features", Map.of("polls", true));
        TenantEntity entity = new TenantEntity();
        entity.setId(tenantId);
        entity.setConfig(config);

        // When
        Tenant tenant = mapper.toDomain(entity);

        // Then
        assertThat(tenant).isNotNull();
        assertThat(tenant.getId()).isEqualTo(tenantId);
        assertThat(tenant.getConfig()).containsEntry("theme", "dark");
        assertThat(tenant.getConfig()).containsKey("features");
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Given
        Map<String, Object> config = Map.of("language", "en", "timezone", "UTC");
        Tenant tenant = com.openforum.domain.factory.TenantFactory.create("tenant-2", config);

        // When
        TenantEntity entity = mapper.toEntity(tenant);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo("tenant-2");
        assertThat(entity.getConfig()).containsEntry("language", "en");
        assertThat(entity.getConfig()).containsEntry("timezone", "UTC");
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        Tenant tenant = mapper.toDomain(null);

        // Then
        assertThat(tenant).isNull();
    }

    @Test
    void toEntity_shouldReturnNull_whenTenantIsNull() {
        // When
        TenantEntity entity = mapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }
}
