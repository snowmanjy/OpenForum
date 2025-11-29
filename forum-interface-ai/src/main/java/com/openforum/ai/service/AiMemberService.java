package com.openforum.ai.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiMemberService {

    private final MemberRepository memberRepository;
    private static final String AI_MEMBER_EXTERNAL_ID = "ai-assistant";



    public AiMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member getOrCreateAiMember(String tenantId) {
        String aiExternalId = "ai-bot-" + tenantId;
        return memberRepository.findByExternalId(tenantId, aiExternalId)
                .orElseGet(() -> {
                    Member aiMember = Member.create(aiExternalId, "ai@openforum.com", "AI Assistant", true);
                    memberRepository.save(aiMember);
                    return aiMember;
                });
    }
}
