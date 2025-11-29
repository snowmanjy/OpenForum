package com.openforum.infra.jpa.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.domain.events.PostImportedEvent;
import com.openforum.domain.events.ThreadImportedEvent;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.mapper.ThreadMapper;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ThreadRepositoryImpl implements ThreadRepository {

    private final ThreadJpaRepository threadJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ThreadMapper threadMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public ThreadRepositoryImpl(ThreadJpaRepository threadJpaRepository,
            OutboxEventJpaRepository outboxEventJpaRepository,
            ThreadMapper threadMapper,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate) {
        this.threadJpaRepository = threadJpaRepository;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.threadMapper = threadMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves a Thread aggregate and its domain events atomically.
     * <p>
     * <strong>ACID Guarantees:</strong>
     * <ul>
     * <li><strong>Atomicity:</strong> Thread entity and all outbox events are saved
     * in a single transaction.
     * If any event serialization or save fails, the entire transaction rolls
     * back.</li>
     * <li><strong>Consistency:</strong> Thread state and events are always in sync
     * - no partial saves.</li>
     * <li><strong>Isolation:</strong> Default isolation level (READ_COMMITTED)
     * prevents dirty reads.</li>
     * <li><strong>Durability:</strong> Once committed, both thread and events are
     * persisted.</li>
     * </ul>
     * <p>
     * <strong>Failure Scenarios:</strong>
     * <ul>
     * <li>If {@link ThreadJpaRepository#save} fails → entire transaction rolls
     * back</li>
     * <li>If event serialization fails → {@link RuntimeException} thrown,
     * transaction rolls back</li>
     * <li>If {@link OutboxEventJpaRepository#save} fails → transaction rolls
     * back</li>
     * </ul>
     *
     * @param thread the thread aggregate to save with its domain events
     * @throws RuntimeException if event serialization fails or any database
     *                          operation fails,
     *                          triggering automatic rollback
     */
    @Override
    @Transactional
    public void save(Thread thread) {
        // Step 1: Save Thread Entity (if this fails, entire transaction rolls back)
        ThreadEntity entity = threadMapper.toEntity(thread);
        threadJpaRepository.save(entity);

        // Step 2: Poll and Save Events atomically
        // If ANY event fails to serialize or save, the ENTIRE transaction (including
        // Thread save) rolls back
        List<Object> events = thread.pollEvents();
        events.stream()
                .map(this::toOutboxEntity) // Throws RuntimeException if serialization fails
                .forEach(outboxEventJpaRepository::save); // Any save failure triggers rollback
    }

    @Override
    public Optional<Thread> findById(UUID id) {
        return threadJpaRepository.findById(id)
                .map(threadMapper::toDomain);
    }

    /**
     * Batch saves multiple Thread aggregates and their domain events atomically
     * using JDBC.
     * <p>
     * This implementation uses {@link JdbcTemplate} to perform efficient batch
     * INSERTs,
     * bypassing JPA's first-level cache and "isNew" checks. This is critical for
     * bulk imports
     * where entities have pre-assigned IDs (which JPA would interpret as updates).
     * <p>
     * <strong>Scope:</strong>
     * <ul>
     * <li>Inserts Threads</li>
     * <li>Inserts Posts (which are part of the Thread aggregate)</li>
     * <li>Inserts Outbox Events (if any)</li>
     * </ul>
     * 
     * @param threads List of thread aggregates to save
     * @throws RuntimeException if any database operation fails
     */
    @Override
    @Transactional
    public void saveAll(List<Thread> threads) {
        if (threads.isEmpty()) {
            return;
        }

        // 1. Batch Insert Threads
        String threadSql = """
                INSERT INTO threads (id, tenant_id, author_id, title, status, metadata, version)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                """;

        jdbcTemplate.batchUpdate(threadSql, threads, threads.size(), (ps, thread) -> {
            ps.setObject(1, thread.getId());
            ps.setString(2, thread.getTenantId());
            ps.setObject(3, thread.getAuthorId());
            ps.setString(4, thread.getTitle());
            ps.setString(5, thread.getStatus().name());
            try {
                ps.setString(6, objectMapper.writeValueAsString(thread.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize thread metadata", e);
            }
            ps.setObject(7, thread.getVersion() != null ? thread.getVersion() : 0L);
        });

        // 2. Batch Insert Posts
        // Flatten all posts from all threads into a single list
        List<com.openforum.domain.aggregate.Post> allPosts = threads.stream()
                .flatMap(t -> t.getPosts().stream())
                .toList();

        if (!allPosts.isEmpty()) {
            String postSql = """
                    INSERT INTO posts (id, thread_id, author_id, content, reply_to_post_id, metadata, version)
                    VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                    """;

            jdbcTemplate.batchUpdate(postSql, allPosts, allPosts.size(), (ps, post) -> {
                ps.setObject(1, post.getId());
                ps.setObject(2, post.getThreadId());
                ps.setObject(3, post.getAuthorId());
                ps.setString(4, post.getContent());
                ps.setObject(5, post.getReplyToPostId()); // Handles null automatically
                try {
                    ps.setString(6, objectMapper.writeValueAsString(post.getMetadata()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize post metadata", e);
                }
                ps.setObject(7, post.getVersion() != null ? post.getVersion() : 0L);
            });
        }

        // 3. Batch Insert Events
        // We need to insert two types of events:
        // A. Domain events that were already in the outbox (usually empty for imports,
        // but good to keep)
        // B. "Imported" events for Data Lake sync (ThreadImportedEvent,
        // PostImportedEvent)

        List<OutboxEventEntity> allEvents = new ArrayList<>();

        // A. Existing Domain Events
        allEvents.addAll(threads.stream()
                .flatMap(thread -> thread.pollEvents().stream())
                .map(this::toOutboxEntity)
                .toList());

        // B. Generate Sync Events for Data Lake
        Instant now = Instant.now();
        for (Thread thread : threads) {
            // ThreadImportedEvent
            ThreadImportedEvent threadEvent = new ThreadImportedEvent(
                    thread.getId(),
                    thread.getTenantId(),
                    thread.getAuthorId(),
                    thread.getTitle(),
                    now);
            allEvents.add(toOutboxEntity(threadEvent));

            // PostImportedEvent for each post
            for (com.openforum.domain.aggregate.Post post : thread.getPosts()) {
                PostImportedEvent postEvent = new PostImportedEvent(
                        post.getId(),
                        post.getThreadId(),
                        post.getAuthorId(),
                        post.getContent(),
                        false, // isBot is not persisted in Post aggregate, defaulting to false
                        now);
                allEvents.add(toOutboxEntity(postEvent));
            }
        }

        if (!allEvents.isEmpty()) {
            String eventSql = """
                    INSERT INTO outbox_events (id, aggregate_id, type, payload, created_at)
                    VALUES (?, ?, ?, ?::jsonb, ?)
                    """;

            jdbcTemplate.batchUpdate(eventSql, allEvents, allEvents.size(), (ps, event) -> {
                ps.setObject(1, event.getId());
                ps.setObject(2, event.getAggregateId()); // Might be null
                ps.setString(3, event.getType());
                ps.setString(4, event.getPayload());
                ps.setTimestamp(5, Timestamp.from(event.getCreatedAt()));
            });
        }
    }

    @Override
    public List<Thread> search(String tenantId, String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return threadJpaRepository.search(tenantId, query, pageRequest)
                .map(threadMapper::toDomain)
                .getContent();
    }

    private OutboxEventEntity toOutboxEntity(Object event) {
        try {
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setId(UUID.randomUUID());
            // Assuming event has aggregateId, but for generic Object we might need
            // reflection or specific interface
            // For now, we'll rely on the event payload containing the ID.
            // Ideally, DomainEvent interface would expose aggregateId.
            // Since we don't have a common interface yet, we'll skip aggregateId for now or
            // extract if possible.
            // Let's set aggregateId to null or try to find it.
            // Given the constraints, we will just serialize the payload.

            entity.setType(event.getClass().getSimpleName());
            entity.setPayload(objectMapper.writeValueAsString(event));
            entity.setCreatedAt(java.time.Instant.now());
            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
