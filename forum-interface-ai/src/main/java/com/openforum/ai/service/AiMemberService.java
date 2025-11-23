package com.openforum.ai.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiMemberService {

    private final MemberRepository memberRepository;
    private static final String AI_MEMBER_EXTERNAL_ID = "ai-assistant";
    private static final String AI_MEMBER_NAME = "AI Assistant";

    public AiMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member getOrCreateAiMember() {
        Optional<Member> existingMember = memberRepository.findByExternalId(AI_MEMBER_EXTERNAL_ID);

        if (existingMember.isPresent()) {
            return existingMember.get();
        }

        // Create new AI member with isBot=true
        Member aiMember = Member.create(
                AI_MEMBER_EXTERNAL_ID,
                "ai@forum.local",
                AI_MEMBER_NAME,
                true // isBot
        );

        return memberRepository.save(aiMember);
    }
}
