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
        entity.setMentionedMemberIds(domain.getMentionedMemberIds());
        entity.setPostNumber(domain.getPostNumber());
        entity.setScore(domain.getScore());
        entity.setDeleted(domain.isDeleted());
        entity.setEmbedding(domain.getEmbedding());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setLastModifiedAt(domain.getLastModifiedAt());
        entity.setLastModifiedBy(domain.getLastModifiedBy());
        entity.setCreatedBy(domain.getCreatedBy());

        // createdAt handled by entity/listener or manual set if needed
        // entity.setCreatedAt(domain.getCreatedAt());

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
                .createdAt(entity.getCreatedAt())
                .mentionedMemberIds(entity.getMentionedMemberIds())
                .postNumber(entity.getPostNumber())
                .isDeleted(entity.getDeleted())
                .score(entity.getScore())
                .embedding(entity.getEmbedding())
                .deletedAt(entity.getDeletedAt())
                .lastModifiedAt(entity.getLastModifiedAt())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public void updateEntity(Post domain, PostEntity target) {
        // ID and TenantID normally shouldn't change
        target.setThreadId(domain.getThreadId());
        target.setAuthorId(domain.getAuthorId());
        target.setContent(domain.getContent());
        target.setVersion(domain.getVersion());
        target.setReplyToPostId(domain.getReplyToPostId());
        target.setMetadata(domain.getMetadata());
        target.setMentionedMemberIds(domain.getMentionedMemberIds());
        target.setPostNumber(domain.getPostNumber());
        target.setScore(domain.getScore());
        target.setDeleted(domain.isDeleted());
        target.setDeletedAt(domain.getDeletedAt());
        target.setLastModifiedAt(domain.getLastModifiedAt());
        target.setLastModifiedBy(domain.getLastModifiedBy());

        // Only update embedding if present in domain (it might be null if not loaded)
        if (domain.getEmbedding() != null) {
            target.setEmbedding(domain.getEmbedding());
        }

        // CreatedBy usually doesn't change
    }
}
