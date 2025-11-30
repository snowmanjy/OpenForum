package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Tenant;
import com.openforum.domain.repository.TenantRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Import({ TenantRepositoryImpl.class, com.openforum.infra.jpa.mapper.TenantMapper.class, JpaTestConfig.class })
class TenantRepositoryImplTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void should_save_and_retrieve_tenant() {
        // Given
        String tenantId = "tenant-1";
        Tenant tenant = com.openforum.domain.factory.TenantFactory.create(tenantId, Map.of("key", "value"));

        // When
        tenantRepository.save(tenant);
        Optional<Tenant> retrieved = tenantRepository.findById(tenantId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(tenantId);
        assertThat(retrieved.get().getConfig()).containsEntry("key", "value");
    }

    @Test
    void should_return_empty_when_tenant_not_found() {
        // When
        Optional<Tenant> retrieved = tenantRepository.findById("non-existent");

        // Then
        assertThat(retrieved).isEmpty();
    }
}
