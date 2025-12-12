package com.openforum.infra.jpa.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.repository.PostRepository;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.mapper.PostMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final PostMapper postMapper;
    private final ObjectMapper objectMapper;

    public PostRepositoryImpl(PostJpaRepository postJpaRepository,
            OutboxEventJpaRepository outboxEventJpaRepository,
            PostMapper postMapper,
            ObjectMapper objectMapper) {
        this.postJpaRepository = postJpaRepository;
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.postMapper = postMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Post save(Post post) {
        // 1. Save Post Entity
        PostEntity entity = postJpaRepository.findById(post.getId())
                .map(existing -> {
                    postMapper.updateEntity(post, existing);
                    return existing;
                })
                .orElseGet(() -> postMapper.toEntity(post));

        postJpaRepository.save(entity);

        // 2. Poll and Save Events
        List<Object> events = post.pollEvents();
        events.stream()
                .map(this::toOutboxEntity)
                .forEach(outboxEventJpaRepository::save);

        return post;
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return postJpaRepository.findById(id)
                .map(postMapper::toDomain);
    }

    @Override
    public List<Post> findByThreadId(UUID threadId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
        return postJpaRepository.findByThreadId(threadId, pageRequest).stream()
                .map(postMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Post> findByIdAndTenantId(UUID id, String tenantId) {
        return postJpaRepository.findByIdAndTenantId(id, tenantId)
                .map(postMapper::toDomain);
    }

    @Override
    public List<Post> findByTenantId(String tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return postJpaRepository.findByTenantId(tenantId, pageRequest)
                .map(postMapper::toDomain)
                .getContent();
    }

    @Override
    public int deleteBatch(java.time.Instant cutoff, int limit) {
        return postJpaRepository.deleteBatch(cutoff, limit);
    }

    private OutboxEventEntity toOutboxEntity(Object event) {
        try {
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setId(UUID.randomUUID());
            entity.setType(event.getClass().getSimpleName());
            entity.setPayload(objectMapper.writeValueAsString(event));
            entity.setCreatedAt(java.time.Instant.now());
            return entity;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
