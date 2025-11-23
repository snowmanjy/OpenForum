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
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus());
        entity.setMetadata(domain.getMetadata());
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public Thread toDomain(ThreadEntity entity) {
        return Thread.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .authorId(entity.getAuthorId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .metadata(entity.getMetadata())
                .version(entity.getVersion())
                .build();
    }
}
