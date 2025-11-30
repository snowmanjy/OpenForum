package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Member;
import com.openforum.infra.jpa.entity.MemberEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperTest {

    private final MemberMapper mapper = new MemberMapper();

    @Test
    void toDomain_shouldMapAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        MemberEntity entity = new MemberEntity(id, "ext-123", "test@example.com", "Test User", false, "tenant-1");

        // When
        Member member = mapper.toDomain(entity);

        // Then
        assertThat(member).isNotNull();
        assertThat(member.getId()).isEqualTo(id);
        assertThat(member.getExternalId()).isEqualTo("ext-123");
        assertThat(member.getEmail()).isEqualTo("test@example.com");
        assertThat(member.getName()).isEqualTo("Test User");
        assertThat(member.isBot()).isFalse();
    }

    @Test
    void toEntity_shouldMapAllFields() {
        // Given
        Member member = Member.create("ext-456", "john@example.com", "John Doe", false);

        // When
        MemberEntity entity = mapper.toEntity(member);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(member.getId());
        assertThat(entity.getExternalId()).isEqualTo("ext-456");
        assertThat(entity.getEmail()).isEqualTo("john@example.com");
        assertThat(entity.getName()).isEqualTo("John Doe");
        assertThat(entity.isBot()).isFalse();
    }

    @Test
    void toDomain_shouldReturnNull_whenEntityIsNull() {
        // When
        Member member = mapper.toDomain(null);

        // Then
        assertThat(member).isNull();
    }

    @Test
    void toEntity_shouldReturnNull_whenMemberIsNull() {
        // When
        MemberEntity entity = mapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }
}
