package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Tag;
import com.openforum.domain.repository.TagRepository;
import com.openforum.infra.jpa.entity.TagEntity;
import com.openforum.infra.jpa.repository.TagJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TagRepositoryImpl implements TagRepository {

    private final TagJpaRepository tagJpaRepository;

    public TagRepositoryImpl(TagJpaRepository tagJpaRepository) {
        this.tagJpaRepository = tagJpaRepository;
    }

    @Override
    public Tag save(Tag tag) {
        TagEntity entity = toEntity(tag);
        TagEntity savedEntity = tagJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Tag> findByName(String tenantId, String name) {
        return tagJpaRepository.findByTenantIdAndName(tenantId, name)
                .map(this::toDomain);
    }

    @Override
    public List<Tag> findByNameStartingWith(String tenantId, String prefix, int limit) {
        return tagJpaRepository.findByNameStartingWith(tenantId, prefix, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementUsageCount(String tenantId, String name) {
        tagJpaRepository.incrementUsageCount(tenantId, name);
    }

    private TagEntity toEntity(Tag tag) {
        TagEntity entity = new TagEntity();
        entity.setId(tag.getId());
        entity.setTenantId(tag.getTenantId());
        entity.setName(tag.getName());
        entity.setUsageCount(tag.getUsageCount());
        return entity;
    }

    private Tag toDomain(TagEntity entity) {
        return Tag.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getUsageCount());
    }
}
