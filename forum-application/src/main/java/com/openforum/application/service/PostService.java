package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.factory.PostFactory;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ThreadRepository threadRepository;

    public PostService(PostRepository postRepository, MemberRepository memberRepository,
            ThreadRepository threadRepository) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.threadRepository = threadRepository;
    }

    /**
     * Creates a reply to an existing thread with pessimistic locking to prevent
     * race conditions.
     * 
     * @param threadId         Target thread ID
     * @param authorId         Author member ID
     * @param tenantId         Tenant ID for multi-tenancy enforcement
     * @param content          Reply content
     * @param replyToPostId    Optional post being replied to
     * @param metadata         Optional metadata
     * @param mentionedUserIds Optional mentioned users
     * @return Created Post
     * @throws IllegalArgumentException if thread or member not found
     * @throws IllegalStateException    if thread is closed
     */
    @Transactional
    public Post createReply(UUID threadId, UUID authorId, String tenantId, String content,
            UUID replyToPostId, Map<String, Object> metadata, List<UUID> mentionedUserIds) {

        // 1. Fetch thread with pessimistic write lock to prevent race conditions
        Thread thread = threadRepository.findByIdWithLock(threadId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        if (thread.getStatus() == ThreadStatus.CLOSED) {
            throw new IllegalStateException("Cannot reply to a closed thread.");
        }

        // 2. Get member to check if bot
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + authorId));

        // 3. Calculate next post number (under lock)
        int nextPostNumber = thread.getPostCount() + 1;

        // 4. Update thread post count
        thread.incrementPostCount();
        threadRepository.save(thread);

        // 5. Create Post with calculated postNumber
        Post post = PostFactory.create(tenantId, threadId, authorId, content, replyToPostId,
                member.isBot(), mentionedUserIds);

        post = Post.builder()
                .id(post.getId())
                .threadId(post.getThreadId())
                .tenantId(post.getTenantId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .replyToPostId(post.getReplyToPostId())
                .metadata(metadata != null ? metadata : Map.of())
                .createdAt(post.getCreatedAt())
                .mentionedUserIds(post.getMentionedUserIds())
                .postNumber(nextPostNumber)
                .isNew(true)
                .isBot(member.isBot())
                .build();

        postRepository.save(post);

        // TODO: Publish PostCreatedEvent for notifications

        return post;
    }

    @Transactional
    public Post createPost(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata, List<UUID> mentionedUserIds) {
        // 1. Verify thread exists
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        if (thread.getStatus() == ThreadStatus.CLOSED) {
            throw new IllegalStateException("Cannot reply to a closed thread.");
        }

        // 2. Get member to check if bot
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 3. Update Thread State (Container Pattern)
        // This acts as our pessimistic/optimistic lock boundary
        thread.incrementPostCount();
        threadRepository.save(thread);

        // 4. Create Post
        Post post = PostFactory.create(thread.getTenantId(), threadId, authorId, content, replyToPostId, member.isBot(),
                mentionedUserIds);

        // Assign post number from the updated thread count
        post = Post.builder()
                .id(post.getId())
                .threadId(post.getThreadId())
                .tenantId(post.getTenantId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .replyToPostId(post.getReplyToPostId())
                .metadata(post.getMetadata())
                .createdAt(post.getCreatedAt())
                .mentionedUserIds(post.getMentionedUserIds())
                .postNumber(thread.getPostCount())
                .isNew(true)
                .isBot(member.isBot())
                .build();

        postRepository.save(post);

        return post;
    }

    @Transactional(readOnly = true)
    public Optional<Post> getPost(String tenantId, UUID id) {
        return postRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Post> getPosts(String tenantId, int page, int size) {
        return postRepository.findByTenantId(tenantId, page, size);
    }

    @Transactional(readOnly = true)
    public List<Post> getPostsByThread(UUID threadId, int limit) {
        return postRepository.findByThreadId(threadId, limit);
    }
}
