package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostJpaRepository extends JpaRepository<PostEntity, UUID> {
    List<PostEntity> findByThreadId(UUID threadId, Pageable pageable);
}
