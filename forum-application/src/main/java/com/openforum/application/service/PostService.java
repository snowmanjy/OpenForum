package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.PostFactory;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ThreadRepository threadRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PostService(PostRepository postRepository, MemberRepository memberRepository,
            ThreadRepository threadRepository, ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.threadRepository = threadRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Post createPost(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata) {
        // 1. Load Thread to enforce invariants (Critical Add)
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));

        if (thread.getStatus() == ThreadStatus.CLOSED) {
            throw new IllegalStateException("Cannot reply to a closed thread.");
        }

        // 2. Get member
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 3. Create and Save
        Post post = PostFactory.create(threadId, authorId, content, replyToPostId, metadata, member.isBot());

        return postRepository.save(post);
    }
}
