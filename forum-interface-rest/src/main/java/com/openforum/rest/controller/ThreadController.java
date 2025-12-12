package com.openforum.rest.controller;

import com.openforum.rest.service.ThreadQueryService;
import com.openforum.application.service.ThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import com.openforum.rest.controller.dto.ThreadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.openforum.shared.api.TenantId;

@RestController
@RequestMapping("/api/v1/threads")
@Tag(name = "Threads", description = "Thread management APIs")
public class ThreadController {

        private final ThreadService threadService;
        private final ThreadQueryService threadQueryService;
        private final MemberRepository memberRepository;

        public ThreadController(ThreadService threadService,
                        ThreadQueryService threadQueryService,
                        MemberRepository memberRepository) {
                this.threadService = threadService;
                this.threadQueryService = threadQueryService;
                this.memberRepository = memberRepository;
        }

        @Operation(summary = "Create Thread", description = "Creates a new thread")
        @PostMapping
        public ResponseEntity<ThreadResponse> createThread(
                        @RequestBody CreateThreadRequest request,
                        @TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                Thread thread = threadService.createThread(tenantId, member.getId(), request.title(),
                                request.content(), request.categoryId());

                // API Aggregation: get author name from the authenticated member
                String authorName = member.getName();

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(new ThreadResponse(
                                                thread.getId(),
                                                thread.getTitle(),
                                                thread.getStatus().name(),
                                                request.content(),
                                                thread.getCreatedAt(),
                                                thread.getAuthorId(),
                                                authorName,
                                                thread.getPostCount()));
        }

        @Operation(summary = "Get Thread", description = "Retrieves thread details by ID with OP content")
        @GetMapping("/{id}")
        public ResponseEntity<ThreadResponse> getThread(@PathVariable UUID id) {
                return threadQueryService.getRichThread(id)
                                .map(result -> new ThreadResponse(
                                                result.id(),
                                                result.title(),
                                                result.status(),
                                                result.content(),
                                                result.createdAt(),
                                                result.authorId(),
                                                result.authorName(),
                                                result.postCount()))
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @Operation(summary = "List Threads", description = "Retrieves a list of threads with OP content for a tenant. Supports optional metadata filtering.")
        @GetMapping
        public ResponseEntity<List<ThreadResponse>> getThreads(
                        @TenantId String tenantId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String metadataKey,
                        @RequestParam(required = false) String metadataValue) {

                List<ThreadResponse> response = threadQueryService.getRichThreads(
                                tenantId, page, size, metadataKey, metadataValue).stream()
                                .map(result -> new ThreadResponse(
                                                result.id(),
                                                result.title(),
                                                result.status(),
                                                result.content(),
                                                result.createdAt(),
                                                result.authorId(),
                                                result.authorName(),
                                                result.postCount()))
                                .toList();

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Update Thread Status", description = "Changes thread status (OPEN/CLOSED). Moderators and Admins only.")
        @PutMapping("/{id}/status")
        public ResponseEntity<ThreadResponse> updateStatus(
                        @PathVariable UUID id,
                        @RequestBody UpdateStatusRequest request,
                        @TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                Thread thread = threadService.updateStatus(
                                id,
                                tenantId,
                                member.getId(),
                                member.getRole(),
                                request.status(),
                                request.reason());

                // Fetch author name
                String authorName = memberRepository.findById(thread.getAuthorId())
                                .map(Member::getName)
                                .orElse(null);

                return ResponseEntity.ok(new ThreadResponse(
                                thread.getId(),
                                thread.getTitle(),
                                thread.getStatus().name(),
                                null, // content not returned for status update
                                thread.getCreatedAt(),
                                thread.getAuthorId(),
                                authorName,
                                thread.getPostCount()));
        }

        /**
         * Request DTO for updating thread status.
         */
        public record UpdateStatusRequest(
                        com.openforum.domain.aggregate.ThreadStatus status,
                        String reason) {
        }
}
