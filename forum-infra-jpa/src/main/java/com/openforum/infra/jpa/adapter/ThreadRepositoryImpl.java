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

    @Override
    @Transactional
    public void save(Thread thread) {
        // 1. Save Thread Entity
        ThreadEntity entity = threadMapper.toEntity(thread);
        threadJpaRepository.save(entity);

        // 2. Poll and Save Events
        List<Object> events = thread.pollEvents();
        events.stream()
                .map(this::toOutboxEntity)
                .forEach(outboxEventJpaRepository::save);
    }

    @Override
    public Optional<Thread> findById(UUID id) {
        return threadJpaRepository.findById(id)
                .map(threadMapper::toDomain);
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
