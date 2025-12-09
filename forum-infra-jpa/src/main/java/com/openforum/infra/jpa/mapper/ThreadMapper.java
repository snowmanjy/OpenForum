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
        entity.setPostCount(domain.getPostCount());
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
                .postCount(entity.getPostCount())
                .build();
    }
}
