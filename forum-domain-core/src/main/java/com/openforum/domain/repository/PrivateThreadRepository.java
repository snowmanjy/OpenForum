package com.openforum.domain.repository;

import com.openforum.domain.aggregate.PrivateThread;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivateThreadRepository {
    void save(PrivateThread privateThread);

    Optional<PrivateThread> findByIdAndParticipantId(UUID id, UUID participantId);

    List<PrivateThread> findByParticipantId(String tenantId, UUID participantId, int page, int size);
}
