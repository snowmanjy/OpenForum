package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, UUID> {
    Optional<MemberEntity> findByExternalId(String externalId);
}
