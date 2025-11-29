package com.openforum.rest.controller;

import com.openforum.application.service.PostService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.rest.controller.dto.CreatePostRequest;
import com.openforum.rest.controller.dto.PostResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/threads/{threadId}/posts")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable UUID threadId,
            @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Member member) {
        // TenantContext.getTenantId() is available if needed for future validation
        Post post = postService.createPost(
                threadId,
                member.getId(),
                request.content(),
                request.replyToPostId(),
                request.metadata(),
                request.mentionedUserIds());

        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(post));
    }
}
