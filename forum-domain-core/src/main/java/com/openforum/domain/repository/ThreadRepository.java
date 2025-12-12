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

    Optional<Thread> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find thread by ID with a pessimistic write lock for race condition
     * prevention.
     * Used when calculating postNumber for replies.
     */
    Optional<Thread> findByIdWithLock(UUID id, String tenantId);

    List<Thread> findByTenantId(String tenantId, int page, int size);

    int deleteBatch(java.time.Instant cutoff, int limit);

    /**
     * Archives threads that have been inactive since the cutoff date.
     * 
     * @param cutoff The cutoff timestamp
     * @return number of threads archived
     */
    int archiveStaleThreads(java.time.Instant cutoff);
}
