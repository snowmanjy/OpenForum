package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Member;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository {
    Member save(Member member);

    Optional<Member> findByExternalId(String externalId);

    Optional<Member> findById(UUID id);
}
