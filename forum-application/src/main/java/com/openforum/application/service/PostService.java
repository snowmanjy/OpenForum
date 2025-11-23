package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.PostFactory;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.PostRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PostService(PostRepository postRepository, MemberRepository memberRepository,
            ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Post createPost(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata) {
        // Get member to check isBot status
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + authorId));

        Post post = PostFactory.create(threadId, authorId, content, replyToPostId, metadata, member.isBot());
        Post savedPost = postRepository.save(post);

        // Publish events
        savedPost.pollEvents().forEach(eventPublisher::publishEvent);

        return savedPost;
    }
}
