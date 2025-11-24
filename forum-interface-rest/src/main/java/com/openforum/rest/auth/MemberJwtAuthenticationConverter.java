package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Converts a validated JWT into a Spring Security Authentication object.
 *
 * This converter implements the "Trusted Parent" pattern with JIT (Just-In-Time) provisioning:
 * - JWT signature has already been validated by Spring Security OAuth2 Resource Server
 * - Extracts user claims (sub, email, name) from the JWT
 * - Automatically creates a Member if one doesn't exist (JIT provisioning)
 * - Returns Authentication with Member as principal
 *
 * Architecture Notes:
 * - This is an Infrastructure concern (forum-interface-rest)
 * - The domain (Member aggregate) remains pure - no knowledge of JWT
 * - Follows Clean Architecture: JWT → Converter → Domain Model
 */
@Component
public class MemberJwtAuthenticationConverter implements Converter<Jwt, Authentication> {

    private final MemberRepository memberRepository;

    public MemberJwtAuthenticationConverter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Authentication convert(Jwt jwt) {
        return extractMemberFromJwt(jwt)
                .map(this::createAuthentication)
                .orElse(null);
    }

    /**
     * Extracts or creates a Member from JWT claims using JIT provisioning.
     *
     * @param jwt The validated JWT token
     * @return Optional containing the Member, or empty if claims are invalid
     */
    private Optional<Member> extractMemberFromJwt(Jwt jwt) {
        String externalId = jwt.getSubject(); // 'sub' claim
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        // JIT Provisioning: Find existing member or create new one
        return Optional.of(
            memberRepository.findByExternalId(externalId)
                    .orElseGet(() -> createNewMember(externalId, email, name))
        );
    }

    /**
     * Creates a new Member using domain factory method.
     * This is the "Just-In-Time Provisioning" - we trust the JWT claims
     * because the signature has already been verified.
     */
    private Member createNewMember(String externalId, String email, String name) {
        Member newMember = Member.create(
                externalId,
                email != null ? email : "unknown@example.com",
                name != null ? name : "Unknown User",
                false // Not a bot
        );
        return memberRepository.save(newMember);
    }

    /**
     * Creates Spring Security Authentication object with Member as principal.
     */
    private Authentication createAuthentication(Member member) {
        Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
        return new UsernamePasswordAuthenticationToken(member, null, authorities);
    }
}
