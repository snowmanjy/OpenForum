package com.openforum.rest.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.projection.ThreadWithOPProjection;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for thread read operations that require rich data (projections,
 * aggregations).
 * This follows CQRS principles where complex reads bypass the domain layer.
 */
@Service
public class ThreadQueryService {

        private final ThreadJpaRepository threadJpaRepository;
        private final MemberRepository memberRepository;

        public ThreadQueryService(ThreadJpaRepository threadJpaRepository, MemberRepository memberRepository) {
                this.threadJpaRepository = threadJpaRepository;
                this.memberRepository = memberRepository;
        }

        /**
         * Retrieves a thread with its OP content by ID.
         *
         * @param id Thread UUID
         * @return Optional containing ThreadQueryResult or empty if not found
         */
        @Transactional(readOnly = true)
        public Optional<ThreadQueryResult> getRichThread(UUID id) {
                return threadJpaRepository.findRichThreadById(id)
                                .map(thread -> {
                                        String authorName = memberRepository.findById(thread.getAuthorId())
                                                        .map(Member::getName)
                                                        .orElse(null);
                                        return new ThreadQueryResult(thread, authorName);
                                });
        }

        /**
         * Retrieves paginated threads with OP content for a tenant.
         *
         * @param tenantId Tenant identifier
         * @param page     Page number (0-indexed)
         * @param size     Page size
         * @return List of ThreadQueryResult with author names resolved
         */
        @Transactional(readOnly = true)
        public List<ThreadQueryResult> getRichThreads(String tenantId, int page, int size) {
                Page<ThreadWithOPProjection> richThreads = threadJpaRepository.findRichThreads(
                                tenantId, PageRequest.of(page, size));

                // Batch fetch author names to avoid N+1
                List<UUID> authorIds = richThreads.getContent().stream()
                                .map(ThreadWithOPProjection::getAuthorId)
                                .distinct()
                                .toList();

                Map<UUID, String> authorNames = memberRepository.findByIds(authorIds).stream()
                                .collect(Collectors.toMap(Member::getId, Member::getName));

                return richThreads.getContent().stream()
                                .map(thread -> new ThreadQueryResult(thread, authorNames.get(thread.getAuthorId())))
                                .toList();
        }

        /**
         * Result record containing thread projection with resolved author name.
         */
        public record ThreadQueryResult(
                        UUID id,
                        String title,
                        String status,
                        String content,
                        java.time.Instant createdAt,
                        UUID authorId,
                        String authorName,
                        Integer postCount) {

                public ThreadQueryResult(ThreadWithOPProjection thread, String authorName) {
                        this(
                                        thread.getId(),
                                        thread.getTitle(),
                                        thread.getStatus(),
                                        thread.getContent(),
                                        thread.getCreatedAt(),
                                        thread.getAuthorId(),
                                        authorName,
                                        thread.getPostCount() != null ? thread.getPostCount() : 0);
                }
        }
}
