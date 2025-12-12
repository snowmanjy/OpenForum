package com.openforum.rest.controller;

import com.openforum.rest.service.PostQueryService;
import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.context.TenantContext;
import com.openforum.rest.controller.dto.CreatePostRequest;
import com.openforum.rest.controller.dto.PageResponse;
import com.openforum.rest.controller.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {

        private final PostService postService;
        private final PostQueryService postQueryService;

        public PostController(PostService postService, PostQueryService postQueryService) {
                this.postService = postService;
                this.postQueryService = postQueryService;
        }

        @Operation(summary = "Create Post", description = "Creates a new post (reply) in a thread")
        @PostMapping("/threads/{threadId}/posts")
        public ResponseEntity<PostResponse> createPost(
                        @PathVariable UUID threadId,
                        @RequestBody CreatePostRequest request,
                        @com.openforum.shared.api.TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                Post post = postService.createReply(
                                threadId,
                                member.getId(),
                                tenantId,
                                request.content(),
                                request.replyToPostId(),
                                request.metadata(),
                                request.mentionedMemberIds());

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

                String tenantId = TenantContext.getTenantId();

                PostQueryService.PostQueryPage queryPage = postQueryService.getPostsByThread(
                                threadId, tenantId, page, size, sort);

                List<PostResponse> content = queryPage.content().stream()
                                .map(result -> new PostResponse(
                                                result.id(),
                                                result.threadId(),
                                                result.authorId(),
                                                result.authorName(),
                                                result.content(),
                                                null, // version not needed for list view
                                                result.replyToPostId(),
                                                null, // metadata not needed for list view
                                                result.createdAt(),
                                                result.postNumber(),
                                                result.score(),
                                                null, // userVote not available in this query
                                                0, // bookmarkCount - not available in this query
                                                false, // isBookmarked - not available in this query
                                                null, // deletedAt
                                                null)) // lastModifiedAt
                                .toList();

                return ResponseEntity.ok(new PageResponse<>(
                                content,
                                queryPage.page(),
                                queryPage.size(),
                                queryPage.totalElements(),
                                queryPage.totalPages(),
                                queryPage.first(),
                                queryPage.last()));
        }

        @Operation(summary = "Update Post", description = "Updates post content. Only the author can edit their own posts.")
        @PutMapping("/posts/{postId}")
        public ResponseEntity<PostResponse> updatePost(
                        @PathVariable UUID postId,
                        @RequestBody UpdatePostRequest request,
                        @com.openforum.shared.api.TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                Post post = postService.updatePost(postId, tenantId, member.getId(), request.content());

                return ResponseEntity.ok(PostResponse.from(post, member.getName()));
        }

        @Operation(summary = "Delete Post", description = "Soft-deletes a post. Only the author can delete their own posts.")
        @DeleteMapping("/posts/{postId}")
        public ResponseEntity<Void> deletePost(
                        @PathVariable UUID postId,
                        @com.openforum.shared.api.TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                postService.deletePost(postId, tenantId, member.getId(), null);

                return ResponseEntity.noContent().build();
        }

        /**
         * Request DTO for updating post content.
         */
        public record UpdatePostRequest(String content) {
        }
}
