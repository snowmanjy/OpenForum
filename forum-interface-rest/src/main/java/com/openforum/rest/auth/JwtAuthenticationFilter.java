package com.openforum.rest.auth;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.rest.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;
    private final RSAPublicKey publicKey;

    public JwtAuthenticationFilter(MemberRepository memberRepository, RSAPublicKey publicKey) {
        this.memberRepository = memberRepository;
        this.publicKey = publicKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                processToken(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Prevent memory leaks and tenant bleeding
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html");
    }

    private void processToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            if (signedJWT.verify(verifier)) {
                String externalId = signedJWT.getJWTClaimsSet().getSubject();
                String email = (String) signedJWT.getJWTClaimsSet().getClaim("email");
                String name = (String) signedJWT.getJWTClaimsSet().getClaim("name");
                String tenantId = (String) signedJWT.getJWTClaimsSet().getClaim("tenant_id");

                // Set Tenant Context
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                } else {
                    // TODO: Reject
                    // Fallback or reject? For now, let's assume default if missing, or handle as
                    // error.
                    // Given constraints, we should probably enforce it, but let's default to
                    // "default-tenant" if missing for backward compat during migration
                    tenantId = "default-tenant";
                    TenantContext.setTenantId(tenantId);
                }

                if (externalId != null) {
                    // 3. Check if Member exists in DB (JIT Provisioning)
                    String finalTenantId = tenantId;
                    Member member = memberRepository.findByExternalId(tenantId, externalId)
                            .orElseGet(() -> {
                                String safeEmail = email != null ? email : externalId + "@placeholder.com";
                                String safeName = name != null ? name : "Unknown User";
                                Member newMember = Member.create(externalId, safeEmail, safeName, false, finalTenantId);
                                try {
                                    memberRepository.save(newMember);
                                    return newMember;
                                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                                    // Race condition: Member was created by another request in the meantime
                                    return memberRepository.findByExternalId(finalTenantId, externalId)
                                            .orElseThrow(() -> new IllegalStateException(
                                                    "Member creation failed and could not be retrieved", e));
                                }
                            });

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            member, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                logger.warn("JWT signature verification failed. Header: " + signedJWT.getHeader() + ", Claims: "
                        + signedJWT.getJWTClaimsSet());
            }
        } catch (Exception e) {
            logger.error("Failed to process JWT: " + e.getMessage(), e);
        }
    }
}
