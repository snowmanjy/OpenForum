package com.openforum.application.service;

import com.openforum.domain.aggregate.Category;
import com.openforum.domain.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_shouldSucceed_whenSlugIsUnique() {
        // Given
        String tenantId = "default-tenant";
        String name = "General";
        String slug = "general";
        String description = "General discussions";
        boolean isReadOnly = false;

        when(categoryRepository.findBySlug(tenantId, slug)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Category category = categoryService.createCategory(tenantId, name, slug, description, isReadOnly);

        // Then
        assertThat(category).isNotNull();
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.getSlug()).isEqualTo(slug);
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.isReadOnly()).isEqualTo(isReadOnly);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_shouldFail_whenSlugAlreadyExists() {
        // Given
        String tenantId = "default-tenant";
        String slug = "general";
        Category existingCategory = Category.reconstitute(UUID.randomUUID(), tenantId, "General", slug, "Existing",
                false);
        when(categoryRepository.findBySlug(tenantId, slug)).thenReturn(Optional.of(existingCategory));

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(tenantId, "New Cat", slug, "New", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category with slug 'general' already exists for this tenant.");
    }

    @Test
    void getCategories_shouldReturnAllCategories() {
        // Given
        String tenantId = "default-tenant";
        Category cat1 = Category.reconstitute(UUID.randomUUID(), tenantId, "General", "general", "General discussions",
                false);
        Category cat2 = Category.reconstitute(UUID.randomUUID(), tenantId, "News", "news", "News and announcements",
                false);
        when(categoryRepository.findAll(tenantId)).thenReturn(List.of(cat1, cat2));

        // When
        List<Category> categories = categoryService.getCategories(tenantId);

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting("name").containsExactly("General", "News");
    }
}
