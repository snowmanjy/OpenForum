package com.openforum.admin.service;

import com.openforum.admin.dto.MemberImportRequest;
import com.openforum.admin.dto.MemberImportResponse;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BulkMemberImportService {

    private final MemberRepository memberRepository;

    public BulkMemberImportService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberImportResponse importMembers(MemberImportRequest request) {
        List<String> externalIds = request.members().stream()
                .map(MemberImportRequest.MemberImportItem::externalId)
                .collect(Collectors.toList());

        List<Member> existingMembers = memberRepository.findAllByExternalIdIn(externalIds);
        Set<String> existingExternalIds = existingMembers.stream()
                .map(Member::getExternalId)
                .collect(Collectors.toSet());

        List<Member> newMembers = new ArrayList<>();
        Map<String, UUID> correlationIdMap = new HashMap<>();

        // Map existing members to correlation IDs
        for (MemberImportRequest.MemberImportItem item : request.members()) {
            if (existingExternalIds.contains(item.externalId())) {
                existingMembers.stream()
                        .filter(m -> m.getExternalId().equals(item.externalId()))
                        .findFirst()
                        .ifPresent(m -> correlationIdMap.put(item.correlationId(), m.getId()));
            } else {
                Member newMember = Member.createImported(
                        item.externalId(),
                        item.email(),
                        item.name(),
                        item.joinedAt());
                newMembers.add(newMember);
                correlationIdMap.put(item.correlationId(), newMember.getId());
            }
        }

        if (!newMembers.isEmpty()) {
            memberRepository.saveAll(newMembers);
        }

        return new MemberImportResponse(newMembers.size(), correlationIdMap);
    }
}
