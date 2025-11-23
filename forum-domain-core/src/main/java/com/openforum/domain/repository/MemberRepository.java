package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Member;
import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByExternalId(String externalId);

    void save(Member member);
}
