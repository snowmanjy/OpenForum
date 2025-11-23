package com.openforum.infra.jpa.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.mapper.ThreadMapper;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ThreadRepositoryImpl implements ThreadRepository {

    private final ThreadJpaRepository threadJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ThreadMapper threadMapper;
    private final ObjectMapper objectMapper;

    public ThreadRepositoryImpl(ThreadJpaRepository threadJpaRepository,
            OutboxEventJpaRepository outboxEventJpaRepository,
            ThreadMapper threadMapper,
            ObjectMapper objectMapper) {
        this.threadJpaRepository = threadJpaRepository;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.threadMapper = threadMapper;
        this.objectMapper = objectMapper;
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
     * Batch saves multiple Thread aggregates and their domain events atomically.
     * <p>
     * For bulk imports, imported threads will have empty event lists (isNew=false),
     * so the outbox will naturally remain empty for historical data.
     * 
     * @param threads List of thread aggregates to save
     * @throws RuntimeException if event serialization fails or any database
     *                          operation fails
     */
    @Override
    @Transactional
    public void saveAll(List<Thread> threads) {
        // Step 1: Batch save all Thread entities
        List<ThreadEntity> entities = threads.stream()
                .map(threadMapper::toEntity)
                .toList();
        threadJpaRepository.saveAll(entities);

        // Step 2: Poll and save events from all threads
        // For imported threads, pollEvents() returns empty list, so outbox is naturally
        // empty
        threads.stream()
                .flatMap(thread -> thread.pollEvents().stream())
                .map(this::toOutboxEntity)
                .forEach(outboxEventJpaRepository::save);
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
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
