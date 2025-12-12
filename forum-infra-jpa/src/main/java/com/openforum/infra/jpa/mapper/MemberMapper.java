package com.openforum.infra.jpa.mapper;

import com.openforum.domain.aggregate.Member;
import com.openforum.infra.jpa.entity.MemberEntity;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Member toDomain(MemberEntity entity) {
        if (entity == null) {
            return null;
        }
        return Member.reconstitute(
                entity.getId(),
                entity.getExternalId(),
                entity.getEmail(),
                entity.getName(),
                entity.isBot(),
                entity.getJoinedAt(),
                entity.getCreatedAt(),
                com.openforum.domain.valueobject.MemberRole.valueOf(entity.getRole()),
                entity.getTenantId(),
                entity.getAvatarUrl(),
                entity.getReputation(),
                entity.getLastModifiedAt(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy());
    }

    public MemberEntity toEntity(Member member) {
        if (member == null) {
            return null;
        }
        return new MemberEntity(
                member.getId(),
                member.getExternalId(),
                member.getEmail(),
                member.getName(),
                member.isBot(),
                member.getTenantId(),
                member.getJoinedAt(),
                member.getRole().name(),
                member.getAvatarUrl(),
                member.getReputation());
    }

    public void updateEntity(Member domain, MemberEntity target) {
        // ID, TenantID, ExternalID, Email (assuming email is immutable key or handled
        // carefully), JoinedAt are usually fixed.
        // But Name, Bot, Role, AvatarUrl, Reputation might change.

        target.setName(domain.getName());
        target.setBot(domain.isBot());
        target.setRole(domain.getRole().name());
        target.setEmail(domain.getEmail());
        target.setAvatarUrl(domain.getAvatarUrl());
        target.setReputation(domain.getReputation());

        // JoinedAt is usually preserved
        // target.setJoinedAt(domain.getJoinedAt());
    }
}
