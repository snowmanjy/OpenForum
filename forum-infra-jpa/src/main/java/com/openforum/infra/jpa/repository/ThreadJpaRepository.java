package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.ThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ThreadJpaRepository extends JpaRepository<ThreadEntity, UUID> {
}
