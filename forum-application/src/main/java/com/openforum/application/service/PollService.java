package com.openforum.application.service;

import com.openforum.application.dto.CreatePollRequest;
import com.openforum.application.dto.PollDto;
import com.openforum.application.dto.VotePollRequest;
import com.openforum.domain.aggregate.Poll;
import com.openforum.domain.factory.PollFactory;
import com.openforum.domain.aggregate.PollVote;
import com.openforum.domain.repository.PollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollFactory pollFactory;

    public PollService(PollRepository pollRepository, PollFactory pollFactory) {
        this.pollRepository = pollRepository;
        this.pollFactory = pollFactory;
    }

    @Transactional
    public UUID createPoll(String tenantId, UUID postId, CreatePollRequest request, UUID createdBy) {
        Poll poll = pollFactory.create(
                tenantId,
                postId,
                request.question(),
                request.options(),
                request.expiresAt(),
                request.allowMultipleVotes(),
                createdBy);
        pollRepository.save(poll);
        return poll.getId();
    }

    @Transactional
    public void castVote(String tenantId, UUID pollId, UUID voterId, VotePollRequest request) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (!poll.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Poll not found in tenant");
        }

        poll.castVote(voterId, request.optionIndex());
        pollRepository.save(poll);
    }

    @Transactional(readOnly = true)
    public PollDto getPoll(String tenantId, UUID pollId, UUID currentMemberId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (!poll.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Poll not found in tenant");
        }

        List<Integer> voteCounts = new ArrayList<>(Collections.nCopies(poll.getOptions().size(), 0));
        for (PollVote vote : poll.getVotes()) {
            int index = vote.getOptionIndex();
            if (index >= 0 && index < voteCounts.size()) {
                voteCounts.set(index, voteCounts.get(index) + 1);
            }
        }

        List<Integer> myVotes = poll.getVotes().stream()
                .filter(v -> v.getVoterId().equals(currentMemberId))
                .map(PollVote::getOptionIndex)
                .collect(Collectors.toList());

        boolean hasVoted = !myVotes.isEmpty();

        return new PollDto(
                poll.getId(),
                poll.getPostId(),
                poll.getQuestion(),
                poll.getOptions(),
                poll.getExpiresAt(),
                poll.isAllowMultipleVotes(),
                poll.getCreatedAt(),
                voteCounts,
                hasVoted,
                myVotes);
    }
}
