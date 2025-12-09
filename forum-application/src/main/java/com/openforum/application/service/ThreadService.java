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

    public ThreadService(ThreadRepository threadRepository,
            com.openforum.domain.repository.PostRepository postRepository) {
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Thread createThread(String tenantId, UUID authorId, String title, String content) {
        // 1. Create Thread (Container)
        Thread thread = ThreadFactory.create(tenantId, authorId, null, title, java.util.Map.of());
        // Initialize postCount to 1 (for the OP)
        thread = Thread.builder()
                .id(thread.getId())
                .tenantId(thread.getTenantId())
                .authorId(thread.getAuthorId())
                .categoryId(thread.getCategoryId())
                .title(thread.getTitle())
                .metadata(thread.getMetadata())
                .version(thread.getVersion())
                .createdAt(thread.getCreatedAt())
                .isNew(true)
                .postCount(1) // OP is first post
                .build();

        threadRepository.save(thread);

        // 2. Create Post (OP)
        // We need to inject PostService or PostRepository here, or emit an event.
        // Given the requirement "Refactor ThreadService.createThread Transactional
        // Scope: The method must now save two records."
        // And "return mapper.toDto(thread, post)" - implies we need to handle it here.
        // But ThreadService doesn't have PostRepository dependency in original file.
        // We should add PostRepository dependency.

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
                .mentionedUserIds(post.getMentionedUserIds())
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
}
