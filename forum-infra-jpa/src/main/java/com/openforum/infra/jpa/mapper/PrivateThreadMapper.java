package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.PrivatePost;
import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.infra.jpa.entity.PrivatePostEntity;
import com.openforum.infra.jpa.entity.PrivateThreadEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrivateThreadMapper {

    public PrivateThreadEntity toEntity(PrivateThread domain) {
        return new PrivateThreadEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getSubject(),
                domain.getLastActivityAt(),
                new java.util.HashSet<>(domain.getParticipantIds()));
    }

    public PrivatePostEntity toEntity(PrivatePost domain) {
        return new PrivatePostEntity(
                domain.getId(),
                domain.getThreadId(),
                domain.getAuthorId(),
                domain.getContent());
    }

    public PrivateThread toDomain(PrivateThreadEntity entity, List<PrivatePostEntity> posts) {
        if (entity == null) {
            return null;
        }
        return PrivateThread.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                new java.util.ArrayList<>(entity.getParticipantIds()),
                entity.getSubject(),
                entity.getCreatedAt(),
                entity.getLastActivityAt(),
                posts.stream().map(this::toDomain).collect(Collectors.toList()));
    }

    public PrivatePost toDomain(PrivatePostEntity entity) {
        return PrivatePost.reconstitute(
                entity.getId(),
                entity.getThreadId(),
                entity.getAuthorId(),
                entity.getContent(),
                entity.getCreatedAt());
    }
}
