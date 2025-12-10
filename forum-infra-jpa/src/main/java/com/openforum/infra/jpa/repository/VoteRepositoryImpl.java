package com.openforum.infra.jpa.repository;

import com.openforum.domain.repository.VoteRepository;
import com.openforum.infra.jpa.entity.PostVoteEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class VoteRepositoryImpl implements VoteRepository {

    private final PostVoteJpaRepository postVoteJpaRepository;
    private final PostJpaRepository postJpaRepository;

    public VoteRepositoryImpl(PostVoteJpaRepository postVoteJpaRepository, PostJpaRepository postJpaRepository) {
        this.postVoteJpaRepository = postVoteJpaRepository;
        this.postJpaRepository = postJpaRepository;
    }

    @Override
    public Optional<VoteRecord> findByPostIdAndUserId(UUID postId, UUID userId) {
        return postVoteJpaRepository.findByPostIdAndUserId(postId, userId)
                .map(entity -> new VoteRecord(entity.getPostId(), entity.getUserId(), entity.getValue()));
    }

    @Override
    public List<VoteRecord> findByPostIdsAndUserId(List<UUID> postIds, UUID userId) {
        return postVoteJpaRepository.findByPostIdInAndUserId(postIds, userId).stream()
                .map(entity -> new VoteRecord(entity.getPostId(), entity.getUserId(), entity.getValue()))
                .toList();
    }

    @Override
    @Transactional
    public void save(UUID postId, UUID userId, String tenantId, int value) {
        PostVoteEntity entity = new PostVoteEntity(postId, userId, tenantId, (short) value);
        postVoteJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void update(UUID postId, UUID userId, int value) {
        postVoteJpaRepository.findByPostIdAndUserId(postId, userId)
                .ifPresent(entity -> {
                    entity.setValue((short) value);
                    postVoteJpaRepository.save(entity);
                });
    }

    @Override
    @Transactional
    public void delete(UUID postId, UUID userId) {
        postVoteJpaRepository.findByPostIdAndUserId(postId, userId)
                .ifPresent(postVoteJpaRepository::delete);
    }

    @Override
    @Transactional
    public void updatePostScore(UUID postId, int delta) {
        postJpaRepository.findById(postId).ifPresent(post -> {
            post.setScore(post.getScore() + delta);
            postJpaRepository.save(post);
        });
    }
}
