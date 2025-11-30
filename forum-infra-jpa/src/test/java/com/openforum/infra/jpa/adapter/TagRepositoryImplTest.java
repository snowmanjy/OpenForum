package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Tag;
import com.openforum.domain.factory.TagFactory;
import com.openforum.domain.repository.TagRepository;
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

import java.util.List;
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
@Import({ TagRepositoryImpl.class, com.openforum.infra.jpa.mapper.TagMapper.class, JpaTestConfig.class })
class TagRepositoryImplTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TagRepository tagRepository;

    @Test
    void should_save_and_retrieve_tag() {
        // Given
        String tenantId = "tenant-1";
        Tag tag = TagFactory.create(tenantId, "java");

        // When
        tagRepository.save(tag);
        Optional<Tag> retrieved = tagRepository.findByName(tenantId, "java");

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("java");
        assertThat(retrieved.get().getUsageCount()).isEqualTo(0);
    }

    @Test
    void should_search_by_name_prefix() {
        // Given
        String tenantId = "tenant-1";
        tagRepository.save(TagFactory.create(tenantId, "java"));
        tagRepository.save(TagFactory.create(tenantId, "javascript"));
        tagRepository.save(TagFactory.create(tenantId, "python"));

        // When
        List<Tag> results = tagRepository.findByNameStartingWith(tenantId, "jav", 10);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactlyInAnyOrder("java", "javascript");
    }

    @Test
    void should_respect_tenant_isolation() {
        // Given
        tagRepository.save(TagFactory.create("tenant-1", "java"));
        tagRepository.save(TagFactory.create("tenant-2", "java"));

        // When
        Optional<Tag> tenant1Tag = tagRepository.findByName("tenant-1", "java");
        Optional<Tag> tenant2Tag = tagRepository.findByName("tenant-2", "java");

        // Then
        assertThat(tenant1Tag).isPresent();
        assertThat(tenant2Tag).isPresent();
        assertThat(tenant1Tag.get().getId()).isNotEqualTo(tenant2Tag.get().getId());
    }
}
