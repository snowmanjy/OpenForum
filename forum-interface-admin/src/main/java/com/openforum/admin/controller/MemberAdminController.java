package com.openforum.admin.controller;

import com.openforum.admin.dto.UpsertMemberRequest;
import com.openforum.application.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/members")
public class MemberAdminController {

    private final MemberService memberService;

    public MemberAdminController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PutMapping
    public ResponseEntity<Void> upsertMember(@RequestBody UpsertMemberRequest request) {
        // Ensure tenant context is set if needed, or pass tenantId explicitly.
        // The request has tenantId.
        // We should probably validate that the tenantId in request matches context if
        // context is present,
        // or just use the one in request since this is an admin API likely called by a
        // system that knows the tenant.
        // Given it's a "SaaS Control Plane" API, it might be calling for any tenant.

        memberService.upsertMember(
                request.externalId(),
                request.email(),
                request.name(),
                request.role(),
                request.tenantId(),
                request.avatarUrl());
        return ResponseEntity.ok().build();
    }
}
