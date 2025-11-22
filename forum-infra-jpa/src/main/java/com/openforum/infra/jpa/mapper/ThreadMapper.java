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
        return new Thread(
                entity.getId(),
                entity.getTenantId(),
                entity.getAuthorId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getMetadata(),
                entity.getVersion());
    }
}
