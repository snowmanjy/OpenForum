package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.repository.MemberJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    public MemberRepositoryImpl(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByExternalId(String externalId) {
        return memberJpaRepository.findByExternalId(externalId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(Member member) {
        memberJpaRepository.save(toEntity(member));
    }

    private Member toDomain(MemberEntity entity) {
        return Member.reconstitute(
                entity.getId(),
                entity.getExternalId(),
                entity.getEmail(),
                entity.getName(),
                entity.isBot());
    }

    private MemberEntity toEntity(Member domain) {
        return new MemberEntity(
                domain.getId(),
                domain.getExternalId(),
                domain.getEmail(),
                domain.getName(),
                domain.isBot());
    }
}
