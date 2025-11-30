package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Thread;
import com.openforum.infra.jpa.entity.ThreadEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadMapperTest {

    private final ThreadMapper mapper = new ThreadMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.now();
        ThreadEntity entity = new ThreadEntity();
        entity.setId(id);
        entity.setCategoryId(categoryId);
        entity.setTenantId("tenant-1");
        entity.setAuthorId(authorId);
        entity.setTitle("Test Title");
        entity.setMetadata(Map.of("sticky", true));
        entity.setCreatedAt(now);

        // When
        Thread thread = mapper.toDomain(entity);

        // Then
        assertThat(thread).isNotNull();
        assertThat(thread.getId()).isEqualTo(id);
        assertThat(thread.getCategoryId()).isEqualTo(categoryId);
        assertThat(thread.getAuthorId()).isEqualTo(authorId);
        assertThat(thread.getTitle()).isEqualTo("Test Title");
        assertThat(thread.getMetadata()).containsEntry("sticky", true);
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        Thread thread = mapper.toDomain(null);

        // Then
        assertThat(thread).isNull();
    }
}
