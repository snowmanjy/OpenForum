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
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/threads/{threadId}/subscriptions")
    public ResponseEntity<Void> subscribe(
            @PathVariable UUID threadId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.subscribe(tenantId, member.getId(), threadId, com.openforum.domain.valueobject.TargetType.THREAD);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/threads/{threadId}/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable UUID threadId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.unsubscribe(tenantId, member.getId(), threadId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/categories/{categoryId}/subscriptions")
    public ResponseEntity<Void> subscribeCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.subscribe(tenantId, member.getId(), categoryId, com.openforum.domain.valueobject.TargetType.CATEGORY);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/categories/{categoryId}/subscriptions")
    public ResponseEntity<Void> unsubscribeCategory(
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal Member member) {

        String tenantId = TenantContext.getTenantId();
        subscriptionService.unsubscribe(tenantId, member.getId(), categoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/threads/{threadId}/subscribers")
    public ResponseEntity<List<UUID>> getSubscribers(@PathVariable UUID threadId) {
        // Internal API - in real world might need special auth, but for now assuming
        // protected by network or role
        // For this task, we expose it as requested
        List<UUID> subscribers = subscriptionService.getSubscribers(threadId);
        return ResponseEntity.ok(subscribers);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<java.util.Map<String, Object>> getMySubscriptions(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String tenantId = TenantContext.getTenantId();
        List<com.openforum.application.dto.SubscriptionDto> subscriptions = subscriptionService
                .getSubscriptionsForUser(tenantId, member.getId(), page, size);

        long total = subscriptionService.countSubscriptionsForUser(member.getId());

        return ResponseEntity.ok(java.util.Map.of(
                "data", subscriptions,
                "page", page,
                "total", total));
    }
}
