package com.openforum.domain.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for post vote operations.
 * Provides an abstraction for the application layer to interact with votes
 * without depending on infrastructure details.
 */
public interface VoteRepository {

    /**
     * Find an existing vote by post and user.
     * 
     * @param postId The post ID
     * @param memberId The user ID
     * @return The vote value (1 or -1) if exists, empty otherwise
     */
    Optional<VoteRecord> findByPostIdAndMemberId(UUID postId, UUID memberId);

    /**
     * Save a new vote.
     * 
     * @param postId   The post ID
     * @param memberId   The user ID
     * @param tenantId The tenant ID
     * @param value    The vote value (1 or -1)
     */
    void save(UUID postId, UUID memberId, String tenantId, int value);

    /**
     * Update an existing vote.
     * 
     * @param postId The post ID
     * @param memberId The user ID
     * @param value  The new vote value (1 or -1)
     */
    void update(UUID postId, UUID memberId, int value);

    /**
     * Delete a vote.
     * 
     * @param postId The post ID
     * @param memberId The user ID
     */
    void delete(UUID postId, UUID memberId);

    /**
     * Atomically update the post score.
     * 
     * @param postId The post ID
     * @param delta  The score delta to apply
     */
    void updatePostScore(UUID postId, int delta);

    /**
     * Find all votes by a user for a list of posts.
     * Used for batch fetching user votes when listing posts.
     * 
     * @param postIds List of post IDs
     * @param memberId  The user ID
     * @return List of vote records
     */
    java.util.List<VoteRecord> findByPostIdsAndMemberId(java.util.List<UUID> postIds, UUID memberId);

    /**
     * A simple record representing a stored vote.
     */
    record VoteRecord(UUID postId, UUID memberId, int value) {
    }
}
