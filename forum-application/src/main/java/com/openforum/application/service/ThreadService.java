package com.openforum.application.service;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.factory.ThreadFactory;
import com.openforum.domain.repository.ThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final com.openforum.domain.repository.PostRepository postRepository;

    private final com.openforum.domain.repository.CategoryRepository categoryRepository;
    private final com.openforum.domain.repository.MemberRepository memberRepository;

    public ThreadService(ThreadRepository threadRepository,
            com.openforum.domain.repository.PostRepository postRepository,
            com.openforum.domain.repository.CategoryRepository categoryRepository,
            com.openforum.domain.repository.MemberRepository memberRepository) {
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Thread createThread(String tenantId, UUID authorId, String title, String content, UUID categoryId) {
        // 0. Validate Author
        com.openforum.domain.aggregate.Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found: " + authorId));

        if (!author.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Member does not belong to this tenant");
        }

        if (author.getRole() == com.openforum.domain.valueobject.MemberRole.BANNED) {
            throw new ForbiddenException("Banned members cannot create threads");
        }

        // 1. Resolve Category
        UUID resolvedCategoryId = categoryId;
        if (resolvedCategoryId == null) {
            // Lazy Seeding: Try to find 'general' category
            com.openforum.domain.aggregate.Category generalCategory = categoryRepository.findBySlug(tenantId, "general")
                    .orElseGet(() -> {
                        // Create General category on the fly if not exists
                        com.openforum.domain.aggregate.Category newCat = com.openforum.domain.factory.CategoryFactory
                                .create(
                                        tenantId, "General Discussion", "general",
                                        "Default category for general discussions",
                                        false, authorId);
                        return categoryRepository.save(newCat);
                    });
            resolvedCategoryId = generalCategory.getId();
        } else {
            // Validate provided category
            if (categoryRepository.findById(resolvedCategoryId).isEmpty()) {
                throw new IllegalArgumentException("Category not found: " + resolvedCategoryId);
            }
        }

        // 2. Create Thread (Container)
        Thread thread = ThreadFactory.create(tenantId, authorId, resolvedCategoryId, title, java.util.Map.of());
        // Initialize postCount to 1 (for the OP)
        thread = Thread.builder()
                .id(thread.getId())
                .tenantId(thread.getTenantId())
                .authorId(thread.getAuthorId())
                .categoryId(thread.getCategoryId()) // validated above
                .title(thread.getTitle())
                .metadata(thread.getMetadata())
                .version(thread.getVersion())
                .createdAt(thread.getCreatedAt())
                .isNew(true)
                .postCount(1) // OP is first post
                .build();

        threadRepository.save(thread);

        // 3. Create Post (OP)
        com.openforum.domain.aggregate.Post post = com.openforum.domain.factory.PostFactory.create(
                thread.getTenantId(),
                thread.getId(),
                authorId,
                content,
                null,
                false, // Todo: check if bot?
                java.util.List.of());

        // Ensure postNumber is 1
        post = com.openforum.domain.aggregate.Post.builder()
                .id(post.getId())
                .threadId(post.getThreadId())
                .tenantId(post.getTenantId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .replyToPostId(post.getReplyToPostId())
                .metadata(post.getMetadata())
                .createdAt(post.getCreatedAt())
                .mentionedMemberIds(post.getMentionedMemberIds())
                .postNumber(1)
                .isNew(true)
                .isBot(false)
                .build();

        postRepository.save(post);
        return thread;
    }

    @Transactional(readOnly = true)
    public Optional<Thread> getThread(UUID id) {
        return threadRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Thread> getThread(String tenantId, UUID id) {
        return threadRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Thread> getThreads(String tenantId, int page, int size) {
        return threadRepository.findByTenantId(tenantId, page, size);
    }

    /**
     * Updates the status of a thread (OPEN/CLOSED).
     * Only Moderators and Admins can change thread status.
     * 
     * @param threadId  The thread to update
     * @param tenantId  Tenant context
     * @param memberId  The user making the change
     * @param userRole  The role of the user (used for authorization)
     * @param newStatus The new status (OPEN or CLOSED)
     * @param reason    Optional reason for the status change
     * @return Updated Thread
     * @throws IllegalArgumentException if thread not found
     * @throws ForbiddenException       if user is not a moderator or admin
     */
    @Transactional
    public Thread updateStatus(UUID threadId, String tenantId, UUID memberId,
            com.openforum.domain.valueobject.MemberRole userRole,
            com.openforum.domain.aggregate.ThreadStatus newStatus, String reason) {

        // Permission check: only MOD or ADMIN can change status
        if (userRole != com.openforum.domain.valueobject.MemberRole.MODERATOR &&
                userRole != com.openforum.domain.valueobject.MemberRole.ADMIN) {
            throw new ForbiddenException("Only moderators and admins can change thread status");
        }

        Thread thread = threadRepository.findByIdAndTenantId(threadId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        if (newStatus == com.openforum.domain.aggregate.ThreadStatus.CLOSED) {
            thread.close(reason, memberId);
        } else {
            thread.open(reason, memberId);
        }

        threadRepository.save(thread);
        return thread;
    }

    /**
     * Exception for forbidden actions (403).
     */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
