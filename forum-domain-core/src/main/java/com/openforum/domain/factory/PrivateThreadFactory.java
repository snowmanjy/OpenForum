package com.openforum.domain.factory;

import com.openforum.domain.aggregate.*;

import com.openforum.domain.repository.MemberRepository;

import java.util.List;
import java.util.UUID;

public class PrivateThreadFactory {

    private final MemberRepository memberRepository;

    public PrivateThreadFactory(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public PrivateThread create(String tenantId, List<UUID> participantIds, String subject) {
        if (!memberRepository.existsAllById(participantIds)) {
            throw new IllegalArgumentException("One or more participants do not exist");
        }
        return PrivateThread.create(tenantId, participantIds, subject);
    }
}
