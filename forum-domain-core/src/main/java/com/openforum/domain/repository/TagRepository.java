package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Tag;

import java.util.List;
import java.util.Optional;

public interface TagRepository {
    Tag save(Tag tag);

    Optional<Tag> findByName(String tenantId, String name);

    List<Tag> findByNameStartingWith(String tenantId, String prefix, int limit);

    void incrementUsageCount(String tenantId, String name);
}
