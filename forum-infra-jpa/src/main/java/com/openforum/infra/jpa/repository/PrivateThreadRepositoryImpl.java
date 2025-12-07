package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.domain.repository.PrivateThreadRepository;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.PrivatePostEntity;
import com.openforum.infra.jpa.entity.PrivateThreadEntity;
import com.openforum.infra.jpa.mapper.PrivateThreadMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PrivateThreadRepositoryImpl implements PrivateThreadRepository {

    private final PrivateThreadJpaRepository privateThreadJpaRepository;
    private final PrivatePostJpaRepository privatePostJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final PrivateThreadMapper privateThreadMapper;
    private final ObjectMapper objectMapper;

    public PrivateThreadRepositoryImpl(PrivateThreadJpaRepository privateThreadJpaRepository,
            PrivatePostJpaRepository privatePostJpaRepository,
            OutboxEventJpaRepository outboxEventJpaRepository,
            PrivateThreadMapper privateThreadMapper,
            ObjectMapper objectMapper) {
        this.privateThreadJpaRepository = privateThreadJpaRepository;
        this.privatePostJpaRepository = privatePostJpaRepository;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.privateThreadMapper = privateThreadMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(PrivateThread privateThread) {
        // Save Thread
        PrivateThreadEntity threadEntity = privateThreadMapper.toEntity(privateThread);
        privateThreadJpaRepository.save(threadEntity);

        // Save Posts (New ones)
        List<PrivatePostEntity> postEntities = privateThread.getPosts().stream()
                .map(privateThreadMapper::toEntity)
                .collect(Collectors.toList());
        privatePostJpaRepository.saveAll(postEntities);

        // Save Outbox Events
        privateThread.pollEvents().forEach(event -> {
            try {
                String payload = objectMapper.writeValueAsString(event);
                OutboxEventEntity outboxEvent = new OutboxEventEntity(
                        UUID.randomUUID(),
                        privateThread.getId(),
                        event.getClass().getSimpleName(),
                        payload,
                        Instant.now());
                outboxEventJpaRepository.save(outboxEvent);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        });
    }

    @Override
    public Optional<PrivateThread> findByIdAndParticipantId(UUID id, UUID participantId) {
        return privateThreadJpaRepository.findById(id)
                .filter(thread -> thread.getParticipantIds().contains(participantId))
                .map(thread -> {
                    List<PrivatePostEntity> posts = privatePostJpaRepository
                            .findByThreadIdOrderByCreatedAtAsc(thread.getId());
                    return privateThreadMapper.toDomain(thread, posts);
                });
    }

    @Override
    public List<PrivateThread> findByParticipantId(String tenantId, UUID participantId, int page, int size) {
        return privateThreadJpaRepository
                .findByTenantIdAndParticipantId(tenantId, participantId, PageRequest.of(page, size))
                .stream()
                .map(thread -> {
                    List<PrivatePostEntity> posts = privatePostJpaRepository
                            .findByThreadIdOrderByCreatedAtAsc(thread.getId());
                    return privateThreadMapper.toDomain(thread, posts);
                })
                .collect(Collectors.toList());
    }
}
