package com.openforum.rest.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.repository.PostJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for post read operations that require rich data (projections,
 * aggregations).
 * This follows CQRS principles where complex reads bypass the domain layer.
 */
@Service
public class PostQueryService {

        private static final int MAX_PAGE_SIZE = 50;

        private final PostJpaRepository postJpaRepository;
        private final MemberRepository memberRepository;

        public PostQueryService(PostJpaRepository postJpaRepository, MemberRepository memberRepository) {
                this.postJpaRepository = postJpaRepository;
                this.memberRepository = memberRepository;
        }

        /**
         * Retrieves paginated posts for a thread with sorting options.
         *
         * @param threadId Thread UUID
         * @param tenantId Tenant identifier
         * @param page     Page number (0-indexed)
         * @param size     Page size (will be capped at MAX_PAGE_SIZE)
         * @param sort     Sort option: "oldest" (chronological) or "top" (by score)
         * @return PostQueryPage containing posts with resolved author names
         */
        @Transactional(readOnly = true)
        public PostQueryPage getPostsByThread(UUID threadId, String tenantId, int page, int size, String sort) {
                int effectiveSize = Math.min(size, MAX_PAGE_SIZE);

                Sort sortOrder;
                if ("top".equalsIgnoreCase(sort)) {
                        sortOrder = Sort.by(
                                        Sort.Order.desc("score"),
                                        Sort.Order.asc("createdAt"));
                } else {
                        sortOrder = Sort.by(Sort.Direction.ASC, "postNumber");
                }

                Page<PostEntity> postPage = postJpaRepository.findByThreadIdAndTenantId(
                                threadId, tenantId, PageRequest.of(page, effectiveSize, sortOrder));

                // Batch fetch author names to avoid N+1
                List<UUID> authorIds = postPage.getContent().stream()
                                .map(PostEntity::getAuthorId)
                                .distinct()
                                .toList();

                Map<UUID, String> authorNames = memberRepository.findByIds(authorIds).stream()
                                .collect(Collectors.toMap(Member::getId, Member::getName));

                List<PostQueryResult> content = postPage.getContent().stream()
                                .map(entity -> PostQueryResult.fromEntity(entity,
                                                authorNames.get(entity.getAuthorId())))
                                .toList();

                return new PostQueryPage(
                                content,
                                postPage.getNumber(),
                                postPage.getSize(),
                                postPage.getTotalElements(),
                                postPage.getTotalPages(),
                                postPage.isFirst(),
                                postPage.isLast());
        }

        /**
         * Result record for a single post with resolved author name.
         */
        public record PostQueryResult(
                        UUID id,
                        UUID threadId,
                        UUID authorId,
                        String authorName,
                        String content,
                        UUID replyToPostId,
                        Integer postNumber,
                        int score,
                        Instant createdAt,
                        boolean deleted) {

                public static PostQueryResult fromEntity(PostEntity entity, String authorName) {
                        boolean isDeleted = Boolean.TRUE.equals(entity.getDeleted());
                        return new PostQueryResult(
                                        entity.getId(),
                                        entity.getThreadId(),
                                        entity.getAuthorId(),
                                        authorName,
                                        isDeleted ? "[deleted]" : entity.getContent(),
                                        entity.getReplyToPostId(),
                                        entity.getPostNumber(),
                                        entity.getScore(),
                                        entity.getCreatedAt(),
                                        isDeleted);
                }
        }

        /**
         * Page result for post queries.
         */
        public record PostQueryPage(
                        List<PostQueryResult> content,
                        int page,
                        int size,
                        long totalElements,
                        int totalPages,
                        boolean first,
                        boolean last) {
        }
}
