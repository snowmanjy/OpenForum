package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Poll;
import com.openforum.infra.jpa.entity.PollEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PollMapperTest {

    private final PollMapper mapper = new PollMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(86400);
        PollEntity entity = new PollEntity(id, postId, "tenant-1", "What color?",
                List.of("Red", "Blue"), expiresAt, true, now);

        // When
        Poll poll = mapper.toDomain(entity);

        // Then
        assertThat(poll).isNotNull();
        assertThat(poll.getId()).isEqualTo(id);
        assertThat(poll.getPostId()).isEqualTo(postId);
        assertThat(poll.getTenantId()).isEqualTo("tenant-1");
        assertThat(poll.getQuestion()).isEqualTo("What color?");
        assertThat(poll.getOptions()).containsExactly("Red", "Blue");
        assertThat(poll.isAllowMultipleVotes()).isTrue();
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        Poll poll = mapper.toDomain(null);

        // Then
        assertThat(poll).isNull();
    }
}
