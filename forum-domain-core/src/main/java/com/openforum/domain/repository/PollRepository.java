package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Poll;

import java.util.Optional;
import java.util.UUID;

public interface PollRepository {
    void save(Poll poll);

    Optional<Poll> findById(UUID id);
}
