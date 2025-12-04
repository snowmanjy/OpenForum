package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.valueobject.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void upsertMember(String externalId, String email, String name, String roleName, String tenantId) {
        // 1. Parse Role (Default to MEMBER if invalid)
        MemberRole role;
        try {
            role = MemberRole.valueOf(roleName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgument
        }
        Optional<Member> existingMember = memberRepository.findByExternalId(tenantId, externalId);

        if (existingMember.isPresent()) {
            Member member = existingMember.get();
            boolean changed = false;

            // 2. Handle Role Change
            if (member.getRole() != role) {
                member = member.promoteTo(role);
                changed = true;
            }

            // 3. Handle Details Change
            if (!member.getEmail().equals(email) || !member.getName().equals(name)) {
                member = member.updateDetails(email, name);
                changed = true;
            }

            if (changed) {
                memberRepository.save(member);
            }

        } else {
            Member newMember = Member.createWithRole(externalId, email, name, false, role, tenantId);
            memberRepository.save(newMember);
        }
    }
}
