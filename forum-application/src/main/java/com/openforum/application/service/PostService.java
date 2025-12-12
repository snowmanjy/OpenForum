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

import com.openforum.application.exception.NotFoundException;
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
         * @param mentionedMemberIds Optional mentioned users
         * @return Created Post
         * @throws IllegalArgumentException if thread or member not found
         * @throws IllegalStateException    if thread is closed
         */
        @Transactional
        public Post createReply(UUID threadId, UUID authorId, String tenantId, String content,
                        UUID replyToPostId, Map<String, Object> metadata, List<UUID> mentionedMemberIds) {

                // 1. Fetch thread with pessimistic write lock to prevent race conditions
                Thread thread = threadRepository.findByIdWithLock(threadId, tenantId)
                                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

                if (thread.isDeleted()) {
                        throw new NotFoundException("Thread not found.");
                }

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
                thread.bumpActivity();
                threadRepository.save(thread);

                // 5. Create Post with calculated postNumber
                Post post = PostFactory.create(tenantId, threadId, authorId, content, replyToPostId,
                                member.isBot(), mentionedMemberIds);

                post = Post.builder()
                                .id(post.getId())
                                .threadId(post.getThreadId())
                                .tenantId(post.getTenantId())
                                .authorId(post.getAuthorId())
                                .content(post.getContent())
                                .replyToPostId(post.getReplyToPostId())
                                .metadata(metadata != null ? metadata : Map.of())
                                .createdAt(post.getCreatedAt())
                                .mentionedMemberIds(post.getMentionedMemberIds())
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
                        Map<String, Object> metadata, List<UUID> mentionedMemberIds) {
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
                thread.bumpActivity();
                threadRepository.save(thread);

                // 4. Create Post
                Post post = PostFactory.create(thread.getTenantId(), threadId, authorId, content, replyToPostId,
                                member.isBot(),
                                mentionedMemberIds);

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
                                .mentionedMemberIds(post.getMentionedMemberIds())
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

        /**
         * Updates a post's content. Only the author can edit their own posts.
         * 
         * @param postId     The post to update
         * @param tenantId   Tenant context
         * @param memberId     The user attempting to edit
         * @param newContent The new content
         * @return Updated Post
         * @throws IllegalArgumentException if post not found
         * @throws IllegalStateException    if user is not the author or post is deleted
         */
        @Transactional
        public Post updatePost(UUID postId, String tenantId, UUID memberId, String newContent) {
                Post post = postRepository.findByIdAndTenantId(postId, tenantId)
                                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

                // Permission check: only author can edit
                if (!post.getAuthorId().equals(memberId)) {
                        throw new ForbiddenException("You can only edit your own posts");
                }

                // Check if post is deleted
                if (post.isDeleted()) {
                        throw new IllegalStateException("Cannot edit a deleted post");
                }

                // Use domain method to edit content (emits event)
                post.editContent(newContent, memberId);

                postRepository.save(post);
                return post;
        }

        /**
         * Soft-deletes a post. Only the author can delete their own posts.
         * Content is preserved in DB for admin audit but will be masked in API
         * response.
         * 
         * @param postId   The post to delete
         * @param tenantId Tenant context
         * @param memberId   The user attempting to delete
         * @param reason   Optional reason for deletion
         * @throws IllegalArgumentException if post not found
         * @throws IllegalStateException    if user is not the author
         */
        @Transactional
        public void deletePost(UUID postId, String tenantId, UUID memberId, String reason) {
                Post post = postRepository.findByIdAndTenantId(postId, tenantId)
                                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

                // Permission check: only author can delete
                if (!post.getAuthorId().equals(memberId)) {
                        throw new ForbiddenException("You can only delete your own posts");
                }

                // Use domain method to delete (emits event, sets deleted flag)
                post.delete(reason, memberId);

                postRepository.save(post);
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
