package com.openforum.rest.controller;

import com.openforum.application.service.VoteService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Votes", description = "Post voting APIs")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(summary = "Vote on a Post", description = "Upvote (1) or Downvote (-1) a post. Voting the same value again removes the vote.")
    @PutMapping("/{postId}/vote")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable UUID postId,
            @RequestBody VoteRequest request,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        int delta = voteService.vote(postId, member.getId(), tenantId, request.value());

        return ResponseEntity.ok(new VoteResponse(delta));
    }

    public record VoteRequest(int value) {
    }

    public record VoteResponse(int scoreDelta) {
    }
}
