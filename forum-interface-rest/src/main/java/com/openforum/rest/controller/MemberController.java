package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Member management APIs for tenant-scoped users")
public class MemberController {

    private final MemberRepository memberRepository;

    public MemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Operation(summary = "Get Member by External ID", description = "Retrieves member details by external ID (e.g. Clerk ID)")
    @GetMapping("/external/{externalId}")
    public ResponseEntity<MemberDto> getMemberByExternalId(@PathVariable String externalId) {
        String tenantId = TenantContext.getTenantId();
        return memberRepository.findByExternalId(tenantId, externalId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get Member by ID", description = "Retrieves member details by internal UUID")
    @GetMapping("/{id}")
    public ResponseEntity<MemberDto> getMemberById(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();
        return memberRepository.findById(id)
                .filter(member -> tenantId == null || tenantId.equals(member.getTenantId()))
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private MemberDto toDto(Member member) {
        return new MemberDto(
                member.getId(),
                member.getExternalId(),
                member.getName(),
                member.getEmail(),
                member.getRole().name(),
                member.getJoinedAt(),
                member.getAvatarUrl(),
                member.getReputation());
    }

    @Operation(summary = "Search Members", description = "Searches for members by handle or name within the tenant")
    @GetMapping("/search")
    public ResponseEntity<List<MemberSummaryDto>> searchMembers(@RequestParam String q) {
        String tenantId = TenantContext.getTenantId();
        List<Member> members = memberRepository.searchByHandleOrName(tenantId, q, 10);
        List<MemberSummaryDto> response = members.stream()
                .map(member -> new MemberSummaryDto(member.getId(), member.getName(), member.getEmail(),
                        member.getAvatarUrl()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Lightweight DTO for search results / mentions.
     */
    public record MemberSummaryDto(UUID id, String name, String email, String avatarUrl) {
    }

    /**
     * Full member profile DTO.
     */
    public record MemberDto(UUID id, String externalId, String name, String email, String role,
            java.time.Instant joinedAt, String avatarUrl, int reputation) {
    }
}
