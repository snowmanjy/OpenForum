package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {
    Post save(Post post);

    Optional<Post> findById(UUID id);

    Optional<Post> findByIdAndTenantId(UUID id, String tenantId);

    List<Post> findByThreadId(UUID threadId, int limit);

    List<Post> findByTenantId(String tenantId, int page, int size);

    int deleteBatch(java.time.Instant cutoff, int limit);
}
