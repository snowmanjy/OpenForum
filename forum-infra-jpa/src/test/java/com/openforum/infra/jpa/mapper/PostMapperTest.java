package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Post;
import com.openforum.infra.jpa.entity.PostEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostMapperTest {

    private final PostMapper mapper = new PostMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Instant now = Instant.now();
        PostEntity entity = new PostEntity();
        entity.setId(id);
        entity.setThreadId(threadId);
        entity.setAuthorId(authorId);
        entity.setContent("Test content");
        entity.setCreatedAt(now);
        entity.setVersion(0L);

        // When
        Post post = mapper.toDomain(entity);

        // Then
        assertThat(post).isNotNull();
        assertThat(post.getId()).isEqualTo(id);
        assertThat(post.getThreadId()).isEqualTo(threadId);
        assertThat(post.getAuthorId()).isEqualTo(authorId);
        assertThat(post.getContent()).isEqualTo("Test content");
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        Post post = mapper.toDomain(null);

        // Then
        assertThat(post).isNull();
    }
}
