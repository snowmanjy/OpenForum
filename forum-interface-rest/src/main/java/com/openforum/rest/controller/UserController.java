package com.openforum.rest.controller;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final MemberRepository memberRepository;

    public UserController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String q) {
        String tenantId = TenantContext.getTenantId();
        List<Member> members = memberRepository.searchByHandleOrName(tenantId, q, 10);
        List<UserDto> response = members.stream()
                .map(member -> new UserDto(member.getId(), member.getName(), member.getEmail()))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record UserDto(UUID id, String name, String email) {
    }
}
