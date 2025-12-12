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
    public void upsertMember(String externalId, String email, String name, String roleName, String tenantId,
            String avatarUrl) {
        // 1. Parse Role (Default to MEMBER if invalid)
        MemberRole role;
        try {
            role = MemberRole.valueOf(roleName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid role: " + roleName);
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
            if (!member.getEmail().equals(email) || !member.getName().equals(name)
                    || (avatarUrl != null && !avatarUrl.equals(member.getAvatarUrl()))) {
                member = member.updateDetails(email, name, avatarUrl);
                changed = true;
            }

            if (changed) {
                memberRepository.save(member);
            }

        } else {
            Member newMember = Member.createWithRole(externalId, email, name, false, role, tenantId);
            if (avatarUrl != null) {
                newMember = newMember.updateDetails(email, name, avatarUrl);
            }
            memberRepository.save(newMember);
        }
    }

    @Transactional
    public Member createOwnerMember(String tenantId, String externalId, String email, String name) {
        Optional<Member> existingMember = memberRepository.findByExternalId(tenantId, externalId);
        if (existingMember.isPresent()) {
            return existingMember.get();
        }
        Member newMember = Member.createWithRole(externalId, email, name, false, MemberRole.ADMIN, tenantId);
        return memberRepository.save(newMember);
    }
}
