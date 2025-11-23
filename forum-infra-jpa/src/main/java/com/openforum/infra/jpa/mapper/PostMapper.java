package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Post;
import com.openforum.infra.jpa.entity.PostEntity;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostEntity toEntity(Post domain) {
        PostEntity entity = new PostEntity();
        entity.setId(domain.getId());
        entity.setThreadId(domain.getThreadId());
        entity.setAuthorId(domain.getAuthorId());
        entity.setContent(domain.getContent());
        entity.setVersion(domain.getVersion());
        entity.setReplyToPostId(domain.getReplyToPostId());
        entity.setMetadata(domain.getMetadata());
        return entity;
    }

    public Post toDomain(PostEntity entity) {
        return Post.builder()
                .id(entity.getId())
                .threadId(entity.getThreadId())
                .authorId(entity.getAuthorId())
                .content(entity.getContent())
                .version(entity.getVersion())
                .replyToPostId(entity.getReplyToPostId())
                .metadata(entity.getMetadata())
                .build();
    }
}
