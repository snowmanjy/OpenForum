package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PollVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollVoteJpaRepository extends JpaRepository<PollVoteEntity, UUID> {
    List<PollVoteEntity> findByPollId(UUID pollId);
}
