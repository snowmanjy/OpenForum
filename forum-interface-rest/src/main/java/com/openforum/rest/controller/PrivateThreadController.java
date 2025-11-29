package com.openforum.rest.controller;

import com.openforum.application.dto.CreatePrivatePostRequest;
import com.openforum.application.dto.CreatePrivateThreadRequest;
import com.openforum.application.dto.PrivateThreadDto;
import com.openforum.application.service.PrivateThreadService;
import com.openforum.rest.security.SecurityContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/private-threads")
public class PrivateThreadController {

    private final PrivateThreadService privateThreadService;

    public PrivateThreadController(PrivateThreadService privateThreadService) {
        this.privateThreadService = privateThreadService;
    }

    @PostMapping
    public ResponseEntity<Void> createThread(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CreatePrivateThreadRequest request,
            UriComponentsBuilder uriBuilder) {
        UUID userId = SecurityContext.getCurrentUserId();
        UUID threadId = privateThreadService.createThread(tenantId, userId, request);

        URI location = uriBuilder.path("/api/v1/private-threads/{id}")
                .buildAndExpand(threadId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PostMapping("/{id}/posts")
    public ResponseEntity<Void> createPost(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @RequestBody CreatePrivatePostRequest request) {
        UUID userId = SecurityContext.getCurrentUserId();
        privateThreadService.addPost(tenantId, id, userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<PrivateThreadDto>> getMyThreads(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityContext.getCurrentUserId();
        List<PrivateThreadDto> threads = privateThreadService.getMyThreads(tenantId, userId, page, size);
        return ResponseEntity.ok(threads);
    }
}
