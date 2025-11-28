package com.openforum.rest.controller;

import com.openforum.application.service.SubscriptionService;
import com.openforum.domain.aggregate.Member;
import com.openforum.rest.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/threads/{threadId}")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @PathVariable UUID threadId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.subscribe(tenantId, member.getId(), threadId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable UUID threadId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.unsubscribe(tenantId, member.getId(), threadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscribers")
    public ResponseEntity<List<UUID>> getSubscribers(@PathVariable UUID threadId) {
        // Internal API - in real world might need special auth, but for now assuming
        // protected by network or role
        // For this task, we expose it as requested
        List<UUID> subscribers = subscriptionService.getSubscribers(threadId);
        return ResponseEntity.ok(subscribers);
    }
}
