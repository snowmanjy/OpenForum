package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Thread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository {
    void save(Thread thread);

    void saveAll(List<Thread> threads);

    List<Thread> search(String tenantId, String query, int page, int size);

    Optional<Thread> findById(UUID id);
}
