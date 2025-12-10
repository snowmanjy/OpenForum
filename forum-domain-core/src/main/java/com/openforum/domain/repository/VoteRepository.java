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
     * @param userId The user ID
     * @return The vote value (1 or -1) if exists, empty otherwise
     */
    Optional<VoteRecord> findByPostIdAndUserId(UUID postId, UUID userId);

    /**
     * Save a new vote.
     * 
     * @param postId   The post ID
     * @param userId   The user ID
     * @param tenantId The tenant ID
     * @param value    The vote value (1 or -1)
     */
    void save(UUID postId, UUID userId, String tenantId, int value);

    /**
     * Update an existing vote.
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @param value  The new vote value (1 or -1)
     */
    void update(UUID postId, UUID userId, int value);

    /**
     * Delete a vote.
     * 
     * @param postId The post ID
     * @param userId The user ID
     */
    void delete(UUID postId, UUID userId);

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
     * @param userId  The user ID
     * @return List of vote records
     */
    java.util.List<VoteRecord> findByPostIdsAndUserId(java.util.List<UUID> postIds, UUID userId);

    /**
     * A simple record representing a stored vote.
     */
    record VoteRecord(UUID postId, UUID userId, int value) {
    }
}
