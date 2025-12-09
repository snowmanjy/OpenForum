package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.context.TenantContext;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class HybridJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final MemberRepository memberRepository;

    public HybridJwtAuthenticationConverter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String externalId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String tenantId = jwt.getClaimAsString("tenant_id");

        // Set Tenant Context
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        } else {
            // Check if already set by header (e.g., via TenantContextFilter)
            String existingTenantId = TenantContext.getTenantId();
            if (existingTenantId != null && !existingTenantId.isEmpty()) {
                tenantId = existingTenantId;
            } else {
                // Fail fast if tenant is undefined
                throw new org.springframework.security.authentication.BadCredentialsException(
                        "No Tenant ID found in JWT or Context. Authentication rejected.");
            }
        }

        // JIT Provisioning
        String finalTenantId = tenantId;
        Member member = memberRepository.findByExternalId(finalTenantId, externalId)
                .orElseGet(() -> {
                    if (email == null) {
                        throw new org.springframework.security.authentication.BadCredentialsException(
                                "Email claim is missing in JWT. Cannot provision member.");
                    }
                    if (name == null) {
                        throw new org.springframework.security.authentication.BadCredentialsException(
                                "Name claim is missing in JWT. Cannot provision member.");
                    }

                    try {
                        Member newMember = Member.create(externalId, email, name, false, finalTenantId);
                        memberRepository.save(newMember);
                        return newMember;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Handle race condition
                        return memberRepository.findByExternalId(finalTenantId, externalId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Member creation failed and could not be retrieved", e));
                    }
                });

        return new UsernamePasswordAuthenticationToken(member, jwt, Collections.emptyList());
    }
}
