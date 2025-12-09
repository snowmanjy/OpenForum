package com.openforum.rest.controller;

import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.context.TenantContext;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.mapper.PostMapper;
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
    private final PostMapper postMapper;
    private final MemberRepository memberRepository;

    public PostController(PostService postService,
            PostJpaRepository postJpaRepository,
            PostMapper postMapper,
            MemberRepository memberRepository) {
        this.postService = postService;
        this.postJpaRepository = postJpaRepository;
        this.postMapper = postMapper;
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

    @Operation(summary = "Get Posts by Thread", description = "Retrieves paginated posts for a thread, sorted chronologically")
    @GetMapping("/threads/{threadId}/posts")
    public ResponseEntity<PageResponse<PostResponse>> getPostsByThread(
            @PathVariable UUID threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Enforce maximum page size
        int effectiveSize = Math.min(size, MAX_PAGE_SIZE);

        // Get tenantId from context
        String tenantId = TenantContext.getTenantId();

        // Execute tenant-aware, paginated query sorted by postNumber ASC
        Page<PostEntity> postPage = postJpaRepository.findByThreadIdAndTenantIdOrderByPostNumberAsc(
                threadId, tenantId, PageRequest.of(page, effectiveSize));

        // Convert entities to domain objects
        List<Post> posts = postPage.getContent().stream()
                .map(postMapper::toDomain)
                .toList();

        // API Aggregation: batch fetch all author IDs to avoid N+1
        List<UUID> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        Map<UUID, String> authorNames = authorIds.stream()
                .map(id -> memberRepository.findById(id).orElse(null))
                .filter(m -> m != null)
                .collect(Collectors.toMap(Member::getId, Member::getName));

        // Map to response DTOs with author names
        List<PostResponse> content = posts.stream()
                .map(post -> PostResponse.from(post, authorNames.get(post.getAuthorId())))
                .toList();

        return ResponseEntity.ok(PageResponse.of(postPage, content));
    }
}
