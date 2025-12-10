package com.openforum.application.service;

import com.openforum.domain.repository.VoteRepository;
import com.openforum.domain.repository.VoteRepository.VoteRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class VoteService {

    private final VoteRepository voteRepository;

    public VoteService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    /**
     * Cast or update a vote on a post.
     * 
     * @param postId   The post to vote on
     * @param userId   The user casting the vote
     * @param tenantId The tenant context
     * @param value    The vote value: 1 (upvote), -1 (downvote)
     * @return The resulting score delta applied to the post
     */
    @Transactional
    public int vote(UUID postId, UUID userId, String tenantId, int value) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }

        Optional<VoteRecord> existingVote = voteRepository.findByPostIdAndUserId(postId, userId);

        if (existingVote.isEmpty()) {
            // Scenario A: New Vote
            voteRepository.save(postId, userId, tenantId, value);
            voteRepository.updatePostScore(postId, value);
            return value;
        }

        VoteRecord vote = existingVote.get();
        int existingValue = vote.value();

        if (existingValue == value) {
            // Scenario C: Un-vote (clicking same vote removes it)
            voteRepository.delete(postId, userId);
            voteRepository.updatePostScore(postId, -existingValue);
            return -existingValue;
        } else {
            // Scenario B: Change Vote (e.g., Up to Down)
            int delta = value - existingValue; // e.g., -1 - 1 = -2 or 1 - (-1) = 2
            voteRepository.update(postId, userId, value);
            voteRepository.updatePostScore(postId, delta);
            return delta;
        }
    }

    /**
     * Get the current user's vote on a post.
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return 1, -1, or 0 (no vote)
     */
    public int getUserVote(UUID postId, UUID userId) {
        return voteRepository.findByPostIdAndUserId(postId, userId)
                .map(VoteRecord::value)
                .orElse(0);
    }
}
