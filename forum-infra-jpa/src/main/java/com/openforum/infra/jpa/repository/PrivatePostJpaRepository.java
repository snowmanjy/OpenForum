package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PrivatePostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrivatePostJpaRepository extends JpaRepository<PrivatePostEntity, UUID> {
    List<PrivatePostEntity> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
}
