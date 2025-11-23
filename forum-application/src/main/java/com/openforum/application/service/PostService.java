package com.openforum.application.service;

import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.PostFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class PostService {

    @Transactional
    public Post createPost(UUID threadId, UUID authorId, String content, UUID replyToPostId,
            Map<String, Object> metadata) {
        return PostFactory.create(threadId, authorId, content, replyToPostId, metadata);
    }
}
