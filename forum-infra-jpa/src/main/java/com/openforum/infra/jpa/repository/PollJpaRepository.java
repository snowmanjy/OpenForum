package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PollJpaRepository extends JpaRepository<PollEntity, UUID> {
}
