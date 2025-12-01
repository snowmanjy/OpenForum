package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.mapper.MemberMapper;
import com.openforum.infra.jpa.repository.MemberJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;

    public MemberRepositoryImpl(MemberJpaRepository memberJpaRepository, MemberMapper memberMapper) {
        this.memberJpaRepository = memberJpaRepository;
        this.memberMapper = memberMapper;
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity = toEntity(member);
        MemberEntity savedEntity = memberJpaRepository.save(entity);
        return memberMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Member> findByExternalId(String tenantId, String externalId) {
        return memberJpaRepository.findByTenantIdAndExternalId(tenantId, externalId)
                .map(memberMapper::toDomain);
    }

    @Override
    public Optional<Member> findById(UUID id) {
        return memberJpaRepository.findById(id)
                .map(memberMapper::toDomain);
    }

    @Override
    public List<Member> searchByHandleOrName(String tenantId, String query, int limit) {
        return memberJpaRepository.searchByHandleOrName(tenantId, query + "%", PageRequest.of(0, limit))
                .stream()
                .map(memberMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsAllById(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return memberJpaRepository.countByIdIn(ids) == ids.size();
    }

    @Override
    public void saveAll(List<Member> members) {
        List<MemberEntity> entities = members.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        memberJpaRepository.saveAll(entities);
    }

    @Override
    public List<Member> findAllByExternalIdIn(List<String> externalIds) {
        return memberJpaRepository.findAllByExternalIdIn(externalIds)
                .stream()
                .map(memberMapper::toDomain)
                .collect(Collectors.toList());
    }

    private MemberEntity toEntity(Member domain) {
        return new MemberEntity(
                domain.getId(),
                domain.getExternalId(),
                domain.getEmail(),
                domain.getName(),
                domain.isBot(),
                null,
                domain.getJoinedAt());
    }
}
