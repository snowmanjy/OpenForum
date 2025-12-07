package com.openforum.domain.factory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PostFactoryTest {

    @Test
    void shouldThrowExceptionWhenTenantIdIsNull() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> PostFactory.create(null, threadId, authorId, "Content", null, false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Tenant ID cannot be null when creating a Post");
    }

    @Test
    void shouldThrowExceptionWhenTenantIdIsNullWithMetadata() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> PostFactory.create(null, threadId, authorId, "Content", null, Map.of(), false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Tenant ID cannot be null when creating a Post");
    }

    @Test
    void createImported_shouldThrowExceptionWhenTenantIdIsNull() {
        // Given
        UUID postId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> PostFactory.createImported(
                postId,
                null, // null tenantId
                threadId,
                authorId,
                "Content",
                null,
                Map.of(),
                false,
                java.time.Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Tenant ID cannot be null when creating a Post");
    }

    @Test
    void shouldCreatePostWhenTenantIdIsProvided() {
        // Given
        String tenantId = "test-tenant";
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        // When
        var post = PostFactory.create(tenantId, threadId, authorId, "Valid content", null, false, List.of());

        // Then
        assertThat(post).isNotNull();
        assertThat(post.getTenantId()).isEqualTo(tenantId);
    }
}
