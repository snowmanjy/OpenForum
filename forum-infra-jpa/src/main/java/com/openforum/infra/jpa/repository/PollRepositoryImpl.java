package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Poll;
import com.openforum.domain.repository.PollRepository;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.PollEntity;
import com.openforum.infra.jpa.entity.PollVoteEntity;
import com.openforum.infra.jpa.mapper.PollMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PollRepositoryImpl implements PollRepository {

    private final PollJpaRepository pollJpaRepository;
    private final PollVoteJpaRepository pollVoteJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final PollMapper pollMapper;
    private final ObjectMapper objectMapper;

    public PollRepositoryImpl(PollJpaRepository pollJpaRepository,
            PollVoteJpaRepository pollVoteJpaRepository,
            OutboxEventJpaRepository outboxEventJpaRepository,
            PollMapper pollMapper,
            ObjectMapper objectMapper) {
        this.pollJpaRepository = pollJpaRepository;
        this.pollVoteJpaRepository = pollVoteJpaRepository;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.pollMapper = pollMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void save(Poll poll) {
        // Save Poll
        PollEntity pollEntity = pollMapper.toEntity(poll);
        pollJpaRepository.save(pollEntity);

        // Save Votes (New ones)
        List<PollVoteEntity> voteEntities = poll.getVotes().stream()
                .map(pollMapper::toEntity)
                .collect(Collectors.toList());
        pollVoteJpaRepository.saveAll(voteEntities);

        // Save Outbox Events
        poll.pollEvents().forEach(event -> {
            try {
                String payload = objectMapper.writeValueAsString(event);
                OutboxEventEntity outboxEvent = new OutboxEventEntity(
                        UUID.randomUUID(),
                        poll.getId(),
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
    public Optional<Poll> findById(UUID id) {
        return pollJpaRepository.findById(id)
                .map(poll -> {
                    List<PollVoteEntity> votes = pollVoteJpaRepository.findByPollId(poll.getId());
                    return pollMapper.toDomain(poll, votes);
                });
    }
}
