package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.context.TenantContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Component
public class MemberJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final MemberRepository memberRepository;

    public MemberJwtAuthenticationConverter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return extractMemberFromJwt(jwt)
                .map(this::createAuthentication)
                .orElse(null);
    }

    private Optional<Member> extractMemberFromJwt(Jwt jwt) {
        String externalId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        if (externalId == null) {
            return Optional.empty();
        }

        String tenantId = TenantContext.getTenantId();
        return memberRepository.findByExternalId(tenantId, externalId)
                .or(() -> {
                    Member newMember = Member.create(externalId, email, name, false, tenantId);
                    memberRepository.save(newMember);
                    return Optional.of(newMember);
                });
    }

    private AbstractAuthenticationToken createAuthentication(Member member) {
        Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
        return new UsernamePasswordAuthenticationToken(member, null, authorities);
    }
}
