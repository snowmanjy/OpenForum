package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Poll;
import com.openforum.domain.aggregate.PollVote;
import com.openforum.infra.jpa.entity.PollEntity;
import com.openforum.infra.jpa.entity.PollVoteEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PollMapper {

    public PollEntity toEntity(Poll domain) {
        return new PollEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getPostId(),
                domain.getQuestion(),
                domain.getOptions(),
                domain.getExpiresAt(),
                domain.isAllowMultipleVotes());
    }

    public PollVoteEntity toEntity(PollVote domain) {
        return new PollVoteEntity(
                domain.getId(),
                domain.getPollId(),
                domain.getVoterId(),
                domain.getOptionIndex());
    }

    public void updateEntity(Poll domain, PollEntity target) {
        target.setPostId(domain.getPostId());
        target.setQuestion(domain.getQuestion());
        target.setOptions(domain.getOptions());
        target.setExpiresAt(domain.getExpiresAt());
        target.setAllowMultipleVotes(domain.isAllowMultipleVotes());
        // Preserved: createdAt, etc.
    }

    public Poll toDomain(PollEntity entity, List<PollVoteEntity> votes) {
        if (entity == null) {
            return null;
        }
        return Poll.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getPostId(),
                entity.getQuestion(),
                entity.getOptions(),
                entity.getExpiresAt(),
                entity.isAllowMultipleVotes(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedAt(),
                entity.getLastModifiedBy(),
                votes.stream().map(this::toDomain).collect(Collectors.toList()));
    }

    public PollVote toDomain(PollVoteEntity entity) {
        return PollVote.reconstitute(
                entity.getId(),
                entity.getPollId(),
                entity.getVoterId(),
                entity.getOptionIndex(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedAt(),
                entity.getLastModifiedBy());
    }
}
