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
                entity.getJoinedAt());
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
                null,
                member.getJoinedAt());
    }
}
