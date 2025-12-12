package com.openforum.application.listener;

import com.openforum.application.event.TenantCreatedEvent;
import com.openforum.application.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TenantCreatedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TenantCreatedEventListener.class);

    private final CategoryService categoryService;
    private final com.openforum.application.service.MemberService memberService;

    public TenantCreatedEventListener(CategoryService categoryService,
            com.openforum.application.service.MemberService memberService) {
        this.categoryService = categoryService;
        this.memberService = memberService;
    }

    @EventListener
    @Transactional
    public void handleTenantCreated(TenantCreatedEvent event) {
        // 1. Bootstrap the Admin Member first
        if (event.getExternalOwnerId() == null || event.getOwnerEmail() == null) {
            throw new IllegalArgumentException(
                    "External Owner ID and Email must be provided for new tenant bootstrapping");
        }

        com.openforum.domain.aggregate.Member owner = memberService.createOwnerMember(
                event.getTenantId(),
                event.getExternalOwnerId(),
                event.getOwnerEmail(),
                event.getOwnerName() != null ? event.getOwnerName() : "Admin");

        logger.info("Provisioned initial Admin member for tenant {}: {} ({})", event.getTenantId(), owner.getId(),
                owner.getEmail());

        // 2. Create the Category (Owned by the new Admin)
        try {
            categoryService.createCategory(
                    event.getTenantId(),
                    "General Discussion",
                    "general",
                    "Default category for general discussions",
                    false,
                    owner.getId());
            logger.info("Created default 'General Discussion' category for tenant {}", event.getTenantId());
        } catch (IllegalArgumentException e) {
            // Category might already exist (e.g. idempotency), ignore
            logger.warn("Default category 'general' already exists for tenant {}", event.getTenantId());
        }
    }
}
