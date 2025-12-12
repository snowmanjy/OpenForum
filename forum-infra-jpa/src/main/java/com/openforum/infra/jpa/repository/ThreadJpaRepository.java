package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.projection.ThreadWithOPProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreadJpaRepository extends JpaRepository<ThreadEntity, UUID> {
       @Query(value = "SELECT * FROM threads WHERE tenant_id = :tenantId AND status != 'ARCHIVED' AND search_vector @@ plainto_tsquery('english', :query)", nativeQuery = true)
       Page<ThreadEntity> search(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

       Optional<ThreadEntity> findByIdAndTenantId(UUID id, String tenantId);

       Page<ThreadEntity> findByTenantId(String tenantId, Pageable pageable);

       @Override
       @Query("select t from ThreadEntity t where t.id = :id")
       Optional<ThreadEntity> findById(@Param("id") UUID id);

       /**
        * Fetch a thread with a pessimistic write lock to prevent race conditions
        * when calculating postNumber for replies.
        */
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT t FROM ThreadEntity t WHERE t.id = :id AND t.tenantId = :tenantId")
       Optional<ThreadEntity> findByIdWithLock(@Param("id") UUID id, @Param("tenantId") String tenantId);

       /**
        * Fetch threads with their OP content in a single query to prevent N+1.
        */
       @Query(value = """
                     SELECT t.id AS id,
                            t.title AS title,
                            t.status AS status,
                            p.content AS content,
                            t.created_at AS createdAt,
                            t.author_id AS authorId,
                            t.post_count AS postCount
                     FROM threads t
                     LEFT JOIN posts p ON t.id = p.thread_id AND p.post_number = 1
                     WHERE t.tenant_id = :tenantId AND t.status != 'ARCHIVED'
                     ORDER BY t.created_at DESC
                     """, countQuery = "SELECT count(*) FROM threads WHERE tenant_id = :tenantId AND status != 'ARCHIVED'", nativeQuery = true)
       Page<ThreadWithOPProjection> findRichThreads(@Param("tenantId") String tenantId, Pageable pageable);

       /**
        * Fetch threads with their OP content, filtered by metadata key-value pair.
        * Uses the GIN index on the metadata JSONB column for efficient queries.
        */
       @Query(value = """
                     SELECT t.id AS id,
                            t.title AS title,
                            t.status AS status,
                            p.content AS content,
                            t.created_at AS createdAt,
                            t.author_id AS authorId,
                            t.post_count AS postCount
                     FROM threads t
                     LEFT JOIN posts p ON t.id = p.thread_id AND p.post_number = 1
                     WHERE t.tenant_id = :tenantId
                       AND t.status != 'ARCHIVED'
                       AND t.metadata ->> :metadataKey = :metadataValue
                     ORDER BY t.created_at DESC
                     """, countQuery = """
                     SELECT count(*) FROM threads
                     WHERE tenant_id = :tenantId
                       AND status != 'ARCHIVED'
                       AND metadata ->> :metadataKey = :metadataValue
                     """, nativeQuery = true)
       Page<ThreadWithOPProjection> findRichThreadsByMetadata(
                     @Param("tenantId") String tenantId,
                     @Param("metadataKey") String metadataKey,
                     @Param("metadataValue") String metadataValue,
                     Pageable pageable);

       /**
        * Fetch a single thread with its OP content.
        */
       @Query(value = """
                     SELECT t.id AS id,
                            t.title AS title,
                            t.status AS status,
                            p.content AS content,
                            t.created_at AS createdAt,
                            t.author_id AS authorId,
                            t.post_count AS postCount
                     FROM threads t
                     LEFT JOIN posts p ON t.id = p.thread_id AND p.post_number = 1
                     WHERE t.id = :id
                     """, nativeQuery = true)
       Optional<ThreadWithOPProjection> findRichThreadById(@Param("id") UUID id);

       /**
        * Delete batch of soft-deleted threads older than cutoff.
        */
       @org.springframework.data.jpa.repository.Modifying
       @Query(value = "DELETE FROM threads WHERE id IN (SELECT id FROM threads WHERE deleted = true AND deleted_at < :cutoff LIMIT :limit)", nativeQuery = true)
       int deleteBatch(@Param("cutoff") java.time.Instant cutoff, @Param("limit") int limit);

       @org.springframework.data.jpa.repository.Modifying
       @Query(value = "UPDATE threads SET status = 'ARCHIVED' WHERE status = 'OPEN' AND last_activity_at < :cutoff", nativeQuery = true)
       int archiveStaleThreads(@Param("cutoff") java.time.Instant cutoff);
}
