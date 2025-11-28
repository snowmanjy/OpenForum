package com.openforum.rest.controller;

import com.openforum.application.service.CategoryService;
import com.openforum.domain.aggregate.Category;
import com.openforum.rest.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        String tenantId = TenantContext.getTenantId();
        List<Category> categories = categoryService.getCategories(tenantId);
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request) {
        String tenantId = TenantContext.getTenantId();
        Category category = categoryService.createCategory(
                tenantId,
                request.name(),
                request.slug(),
                request.description(),
                request.isReadOnly());
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    public record CreateCategoryRequest(String name, String slug, String description, boolean isReadOnly) {
    }

    public record CategoryResponse(UUID id, String name, String slug, String description, boolean isReadOnly) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getDescription(),
                    category.isReadOnly());
        }
    }
}
