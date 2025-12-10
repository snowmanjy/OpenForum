package com.openforum.rest.controller;

import com.openforum.application.service.ThreadService;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.projection.ThreadWithOPProjection;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import com.openforum.rest.controller.dto.CreateThreadRequest;
import com.openforum.rest.controller.dto.ThreadResponse;
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

import com.openforum.shared.api.TenantId;

@RestController
@RequestMapping("/api/v1/threads")
@Tag(name = "Threads", description = "Thread management APIs")
public class ThreadController {

        private final ThreadService threadService;
        private final ThreadJpaRepository threadJpaRepository;
        private final MemberRepository memberRepository;

        public ThreadController(ThreadService threadService,
                        ThreadJpaRepository threadJpaRepository,
                        MemberRepository memberRepository) {
                this.threadService = threadService;
                this.threadJpaRepository = threadJpaRepository;
                this.memberRepository = memberRepository;
        }

        @Operation(summary = "Create Thread", description = "Creates a new thread")
        @PostMapping
        public ResponseEntity<ThreadResponse> createThread(
                        @RequestBody CreateThreadRequest request,
                        @TenantId String tenantId,
                        @AuthenticationPrincipal Member member) {

                Thread thread = threadService.createThread(tenantId, member.getId(), request.title(),
                                request.content());

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
                return threadJpaRepository.findRichThreadById(id)
                                .map(thread -> {
                                        // API Aggregation: fetch author name
                                        String authorName = memberRepository.findById(thread.getAuthorId())
                                                        .map(Member::getName)
                                                        .orElse(null);
                                        return new ThreadResponse(
                                                        thread.getId(),
                                                        thread.getTitle(),
                                                        thread.getStatus(),
                                                        thread.getContent(),
                                                        thread.getCreatedAt(),
                                                        thread.getAuthorId(),
                                                        authorName,
                                                        thread.getPostCount() != null ? thread.getPostCount() : 0);
                                })
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @Operation(summary = "List Threads", description = "Retrieves a list of threads with OP content for a tenant")
        @GetMapping
        public ResponseEntity<List<ThreadResponse>> getThreads(
                        @TenantId String tenantId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                // Fetch threads with OP content
                Page<ThreadWithOPProjection> richThreads = threadJpaRepository.findRichThreads(
                                tenantId, PageRequest.of(page, size));

                // API Aggregation: batch fetch all author IDs to avoid N+1
                List<UUID> authorIds = richThreads.getContent().stream()
                                .map(ThreadWithOPProjection::getAuthorId)
                                .distinct()
                                .toList();

                Map<UUID, String> authorNames = authorIds.stream()
                                .map(id -> memberRepository.findById(id).orElse(null))
                                .filter(m -> m != null)
                                .collect(Collectors.toMap(Member::getId, Member::getName));

                List<ThreadResponse> response = richThreads.getContent().stream()
                                .map(p -> new ThreadResponse(
                                                p.getId(),
                                                p.getTitle(),
                                                p.getStatus(),
                                                p.getContent(),
                                                p.getCreatedAt(),
                                                p.getAuthorId(),
                                                authorNames.get(p.getAuthorId()),
                                                p.getPostCount() != null ? p.getPostCount() : 0))
                                .toList();

                return ResponseEntity.ok(response);
        }
}
