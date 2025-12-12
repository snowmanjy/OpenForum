package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Thread;
import com.openforum.infra.jpa.entity.ThreadEntity;
import org.springframework.stereotype.Component;

@Component
public class ThreadMapper {

    public ThreadEntity toEntity(Thread domain) {
        ThreadEntity entity = new ThreadEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setAuthorId(domain.getAuthorId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus());
        entity.setMetadata(domain.getMetadata());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setLastActivityAt(domain.getLastActivityAt());
        entity.setPostCount(domain.getPostCount());

        entity.setCreatedBy(domain.getCreatedBy());
        entity.setLastModifiedAt(domain.getLastModifiedAt());
        entity.setLastModifiedBy(domain.getLastModifiedBy());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }

    public Thread toDomain(ThreadEntity entity) {
        if (entity == null) {
            return null;
        }
        return Thread.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .authorId(entity.getAuthorId())
                .categoryId(entity.getCategoryId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .metadata(entity.getMetadata())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .lastActivityAt(entity.getLastActivityAt())
                .postCount(entity.getPostCount())

                .createdBy(entity.getCreatedBy())
                .lastModifiedAt(entity.getLastModifiedAt())
                .lastModifiedBy(entity.getLastModifiedBy())
                .deleted(entity.getDeleted() != null ? entity.getDeleted() : false)
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public void updateEntity(Thread domain, ThreadEntity target) {
        // ID and TenantID and CreatedAt match

        target.setAuthorId(domain.getAuthorId());
        target.setCategoryId(domain.getCategoryId());
        target.setTitle(domain.getTitle());
        target.setStatus(domain.getStatus());
        target.setMetadata(domain.getMetadata());
        target.setVersion(domain.getVersion());
        target.setPostCount(domain.getPostCount());
        target.setLastActivityAt(domain.getLastActivityAt());

        target.setLastModifiedAt(domain.getLastModifiedAt());
        target.setLastModifiedBy(domain.getLastModifiedBy());
        target.setCreatedBy(domain.getCreatedBy());
        target.setDeleted(domain.isDeleted());
        target.setDeletedAt(domain.getDeletedAt());
    }
}
