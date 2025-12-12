package com.openforum.rest.controller;

import com.openforum.application.service.CategoryService;
import com.openforum.domain.aggregate.Category;
import com.openforum.domain.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "List Categories", description = "Retrieves all categories for the current tenant")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        String tenantId = TenantContext.getTenantId();
        List<Category> categories = categoryService.getCategories(tenantId);
        List<CategoryResponse> response = categories.stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Category", description = "Creates a new category")
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.openforum.domain.aggregate.Member member) {
        String tenantId = TenantContext.getTenantId();
        Category category = categoryService.createCategory(
                tenantId,
                request.name(),
                request.slug(),
                request.description(),
                request.isReadOnly(),
                member.getId());
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
