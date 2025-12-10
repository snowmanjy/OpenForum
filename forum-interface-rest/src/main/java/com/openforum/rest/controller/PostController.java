package com.openforum.rest.controller;

import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.context.TenantContext;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.repository.PostJpaRepository;
import com.openforum.rest.controller.dto.CreatePostRequest;
import com.openforum.rest.controller.dto.PageResponse;
import com.openforum.rest.controller.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {

        private static final int MAX_PAGE_SIZE = 50;

        private final PostService postService;
        private final PostJpaRepository postJpaRepository;
        private final MemberRepository memberRepository;

        public PostController(PostService postService,
                        PostJpaRepository postJpaRepository,
                        MemberRepository memberRepository) {
                this.postService = postService;
                this.postJpaRepository = postJpaRepository;
                this.memberRepository = memberRepository;
        }

        @Operation(summary = "Create Post", description = "Creates a new post (reply) in a thread")
        @PostMapping("/threads/{threadId}/posts")
        public ResponseEntity<PostResponse> createPost(
                        @PathVariable UUID threadId,
                        @RequestBody CreatePostRequest request,
                        @com.openforum.shared.api.TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                // Use createReply with pessimistic locking for race condition prevention
                Post post = postService.createReply(
                                threadId,
                                member.getId(),
                                tenantId,
                                request.content(),
                                request.replyToPostId(),
                                request.metadata(),
                                request.mentionedUserIds());

                // API Aggregation: get author name from authenticated member
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(PostResponse.from(post, member.getName()));
        }

        @Operation(summary = "Get Posts by Thread", description = "Retrieves paginated posts for a thread. Sort by 'oldest' (chronological) or 'top' (score desc)")
        @GetMapping("/threads/{threadId}/posts")
        public ResponseEntity<PageResponse<PostResponse>> getPostsByThread(
                        @PathVariable UUID threadId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "oldest") String sort) {

                // Enforce maximum page size
                int effectiveSize = Math.min(size, MAX_PAGE_SIZE);

                // Get tenantId from context
                String tenantId = TenantContext.getTenantId();

                // Build PageRequest with appropriate sort
                org.springframework.data.domain.Sort sortOrder;
                if ("top".equalsIgnoreCase(sort)) {
                        // Primary: High Score first, Secondary: Oldest Post first (stable tie-breaker)
                        sortOrder = org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Order.desc("score"),
                                        org.springframework.data.domain.Sort.Order.asc("createdAt"));
                } else {
                        // Default: oldest first (chronological)
                        sortOrder = org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.ASC, "postNumber");
                }

                // Execute tenant-aware, paginated query with dynamic sorting
                Page<PostEntity> postPage = postJpaRepository.findByThreadIdAndTenantId(
                                threadId, tenantId, PageRequest.of(page, effectiveSize, sortOrder));

                // API Aggregation: batch fetch all author IDs to avoid N+1
                List<UUID> authorIds = postPage.getContent().stream()
                                .map(PostEntity::getAuthorId)
                                .distinct()
                                .toList();

                Map<UUID, String> authorNames = authorIds.stream()
                                .map(id -> memberRepository.findById(id).orElse(null))
                                .filter(m -> m != null)
                                .collect(Collectors.toMap(Member::getId, Member::getName));

                // Map entities directly to response DTOs to preserve score
                List<PostResponse> content = postPage.getContent().stream()
                                .map(entity -> PostResponse.fromEntity(entity, authorNames.get(entity.getAuthorId()),
                                                null))
                                .toList();

                return ResponseEntity.ok(PageResponse.of(postPage, content));
        }
}
