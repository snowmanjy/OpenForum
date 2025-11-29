package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Member save(Member member);

    Optional<Member> findByExternalId(String tenantId, String externalId);

    List<Member> searchByHandleOrName(String tenantId, String query, int limit);

    Optional<Member> findById(UUID id);
}
