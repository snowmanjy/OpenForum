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
        entity.setTenantId(domain.getTenantId());
        entity.setAuthorId(domain.getAuthorId());
        entity.setContent(domain.getContent());
        entity.setVersion(domain.getVersion());
        entity.setReplyToPostId(domain.getReplyToPostId());
        entity.setMetadata(domain.getMetadata());
        entity.setMentionedUserIds(domain.getMentionedUserIds());
        entity.setPostNumber(domain.getPostNumber());
        return entity;
    }

    public Post toDomain(PostEntity entity) {
        if (entity == null) {
            return null;
        }
        return Post.builder()
                .id(entity.getId())
                .threadId(entity.getThreadId())
                .tenantId(entity.getTenantId())
                .authorId(entity.getAuthorId())
                .content(entity.getContent())
                .version(entity.getVersion())
                .replyToPostId(entity.getReplyToPostId())
                .metadata(entity.getMetadata())
                .mentionedUserIds(entity.getMentionedUserIds())
                .postNumber(entity.getPostNumber())
                .build();
    }
}
