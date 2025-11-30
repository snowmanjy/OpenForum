package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Category;
import com.openforum.domain.factory.CategoryFactory;
import com.openforum.domain.repository.CategoryRepository;
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
@Import({ CategoryRepositoryImpl.class, com.openforum.infra.jpa.mapper.CategoryMapper.class, JpaTestConfig.class })
class CategoryRepositoryImplTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void should_save_and_retrieve_category() {
        // Given
        String tenantId = "tenant-1";
        Category category = CategoryFactory.create(tenantId, "General", "general", "General discussions", false);

        // When
        categoryRepository.save(category);
        Optional<Category> retrieved = categoryRepository.findBySlug(tenantId, "general");

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("General");
        assertThat(retrieved.get().getSlug()).isEqualTo("general");
        assertThat(retrieved.get().getDescription()).isEqualTo("General discussions");
    }

    @Test
    void should_findAll_by_tenant() {
        // Given
        String tenantId = "tenant-1";
        categoryRepository.save(CategoryFactory.create(tenantId, "General", "general", "General", false));
        categoryRepository.save(CategoryFactory.create(tenantId, "News", "news", "News", false));
        categoryRepository.save(CategoryFactory.create("tenant-2", "Other", "other", "Other", false));

        // When
        List<Category> categories = categoryRepository.findAll(tenantId);

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting("name").containsExactlyInAnyOrder("General", "News");
    }

    @Test
    void should_return_empty_when_category_not_found() {
        // When
        Optional<Category> retrieved = categoryRepository.findBySlug("tenant-1", "non-existent");

        // Then
        assertThat(retrieved).isEmpty();
    }
}
