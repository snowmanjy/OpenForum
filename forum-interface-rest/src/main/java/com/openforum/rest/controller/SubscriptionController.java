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
        subscriptionService.subscribe(tenantId, member.getId(), threadId);
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
        List<com.openforum.application.dto.SubscriptionWithThreadDto> subscriptions = subscriptionService
                .getSubscriptionsForUser(tenantId, member.getId(), page, size);

        // We need total count for pagination metadata.
        // Ideally SubscriptionService should return a Page object or similar wrapper.
        // For this task, I'll add a count method to service or just return the list for
        // now as per requirement "Response: { data: [...], page, total }"
        // I need to fetch total count.
        // I'll add countSubscriptionsForUser to SubscriptionService or just use
        // repository directly if I could (but I can't here).
        // Let's assume for now I'll just return the data and page, and maybe mock total
        // or fetch it if I update service.
        // Wait, the requirement asked for "total".
        // I should update SubscriptionService to return a Page/Wrapper or add a count
        // method.
        // I added `countByUserId` to repository. I should add
        // `countSubscriptionsForUser` to Service.
        // But I didn't add it to Service yet.
        // I will add it to Service in next step or just use a placeholder total for now
        // to get it compiling, then fix.
        // Actually, I can't easily add it to service without another edit.
        // I'll use a placeholder for total for this step and fix it immediately after.

        long total = subscriptionService.countSubscriptionsForUser(member.getId());

        return ResponseEntity.ok(java.util.Map.of(
                "data", subscriptions,
                "page", page,
                "total", total));
    }
}
