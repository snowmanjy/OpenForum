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
    public Optional<VoteRecord> findByPostIdAndMemberId(UUID postId, UUID memberId) {
        return postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId)
                .map(entity -> new VoteRecord(entity.getPostId(), entity.getMemberId(), entity.getValue()));
    }

    @Override
    public List<VoteRecord> findByPostIdsAndMemberId(List<UUID> postIds, UUID memberId) {
        return postVoteJpaRepository.findByPostIdInAndMemberId(postIds, memberId).stream()
                .map(entity -> new VoteRecord(entity.getPostId(), entity.getMemberId(), entity.getValue()))
                .toList();
    }

    @Override
    @Transactional
    public void save(UUID postId, UUID memberId, String tenantId, int value) {
        PostVoteEntity entity = new PostVoteEntity(postId, memberId, tenantId, (short) value);
        postVoteJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void update(UUID postId, UUID memberId, int value) {
        postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId)
                .ifPresent(entity -> {
                    entity.setValue((short) value);
                    postVoteJpaRepository.save(entity);
                });
    }

    @Override
    @Transactional
    public void delete(UUID postId, UUID memberId) {
        postVoteJpaRepository.findByPostIdAndMemberId(postId, memberId)
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
