package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.infra.jpa.entity.PrivateThreadEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateThreadMapperTest {

    private final PrivateThreadMapper mapper = new PrivateThreadMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        Instant now = Instant.now();
        PrivateThreadEntity entity = new PrivateThreadEntity(id, "tenant-1",
                "Private Discussion", now, now,
                new java.util.HashSet<>(java.util.Arrays.asList(participant1, participant2)));

        // When
        PrivateThread thread = mapper.toDomain(entity, List.of());

        // Then
        assertThat(thread).isNotNull();
        assertThat(thread.getId()).isEqualTo(id);
        assertThat(thread.getSubject()).isEqualTo("Private Discussion");
        assertThat(thread.getParticipantIds()).containsExactlyInAnyOrder(participant1, participant2);
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        PrivateThread thread = mapper.toDomain(null, List.of());

        // Then
        assertThat(thread).isNull();
    }
}
