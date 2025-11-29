package com.openforum.rest.controller;

import com.openforum.application.dto.CreatePollRequest;
import com.openforum.application.dto.PollDto;
import com.openforum.application.dto.VotePollRequest;
import com.openforum.application.service.PollService;
import com.openforum.rest.context.TenantContext;
import com.openforum.rest.security.SecurityContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping("/posts/{postId}/polls")
    public ResponseEntity<Void> createPoll(@PathVariable UUID postId, @RequestBody CreatePollRequest request) {
        String tenantId = TenantContext.getTenantId();
        // Assuming current user is authorized to create poll on post (e.g. author of
        // post)
        // For now, we just create it.
        UUID pollId = pollService.createPoll(tenantId, postId, request);
        return ResponseEntity.created(URI.create("/api/v1/polls/" + pollId)).build();
    }

    @PostMapping("/polls/{pollId}/votes")
    public ResponseEntity<Void> castVote(@PathVariable UUID pollId, @RequestBody VotePollRequest request) {
        String tenantId = TenantContext.getTenantId();
        UUID currentUserId = SecurityContext.getCurrentUserId();
        pollService.castVote(tenantId, pollId, currentUserId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/polls/{pollId}")
    public ResponseEntity<PollDto> getPoll(@PathVariable UUID pollId) {
        String tenantId = TenantContext.getTenantId();
        UUID currentUserId = SecurityContext.getCurrentUserId();
        PollDto pollDto = pollService.getPoll(tenantId, pollId, currentUserId);
        return ResponseEntity.ok(pollDto);
    }
}
