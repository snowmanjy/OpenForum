package com.openforum.rest.controller;

import com.openforum.application.service.ThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import com.openforum.rest.controller.dto.ThreadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import com.openforum.rest.context.TenantContext;

@RestController
@RequestMapping("/api/v1/threads")
@Tag(name = "Threads", description = "Thread management APIs")
public class ThreadController {

    private final ThreadService threadService;

    public ThreadController(ThreadService threadService) {
        this.threadService = threadService;
    }

    @Operation(summary = "Create Thread", description = "Creates a new thread")
    @PostMapping
    public ResponseEntity<ThreadResponse> createThread(
            @RequestBody CreateThreadRequest request,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();

        Thread thread = threadService.createThread(tenantId, member.getId(), request.title());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ThreadResponse(thread.getId(), thread.getTitle(), thread.getStatus().name()));
    }

    @Operation(summary = "Get Thread", description = "Retrieves thread details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ThreadResponse> getThread(@PathVariable UUID id) {
        return threadService.getThread(id)
                .map(thread -> new ThreadResponse(thread.getId(), thread.getTitle(), thread.getStatus().name()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
