package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.valueobject.MemberRole;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.mapper.MemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemberRepositoryImplTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;

    private MemberMapper memberMapper = new MemberMapper();

    private MemberRepositoryImpl memberRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        memberRepository = new MemberRepositoryImpl(memberJpaRepository, memberMapper);
    }

    @Test
    void save_ExistingMember_ShouldUpdateMutableFieldsOnly() {
        // Arrange
        UUID memberId = UUID.randomUUID();
        String tenantId = "tenant-1";

        // Existing entity with joinedAt (immutable)
        MemberEntity existingEntity = new MemberEntity(
                memberId, "ext-123", "old@example.com", "Old Name", false, tenantId, Instant.now(), "USER", null, 0);

        when(memberJpaRepository.findById(memberId)).thenReturn(Optional.of(existingEntity));

        // Domain object with update (e.g. name change)
        // Domain also carries joinedAt, but we want to ensure existing entity instance
        // is reused/updated
        Member updatedDomain = Member.reconstitute(
                memberId,
                "ext123",
                "new@example.com",
                "New Name",
                true,
                Instant.now(), // joinedAt
                Instant.now(), // createdAt
                MemberRole.MODERATOR, // role
                "tenant1",
                null, // avatarUrl
                0, // version
                null, // lastModifiedAt
                UUID.randomUUID(), // createdBy
                UUID.randomUUID() // lastModifiedBy
        ); // Act
        memberRepository.save(updatedDomain);

        // Assert
        ArgumentCaptor<MemberEntity> entityCaptor = ArgumentCaptor.forClass(MemberEntity.class);
        verify(memberJpaRepository).save(entityCaptor.capture());

        MemberEntity savedEntity = entityCaptor.getValue();

        // Verify updates applied
        assertEquals("New Name", savedEntity.getName());
        assertEquals(true, savedEntity.isBot());
        assertEquals("MODERATOR", savedEntity.getRole());
        assertEquals("new@example.com", savedEntity.getEmail());

        // Verify identity preservation
        assertEquals(existingEntity, savedEntity);
    }
}
