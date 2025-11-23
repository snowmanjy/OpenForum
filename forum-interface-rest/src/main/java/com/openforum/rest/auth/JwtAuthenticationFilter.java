package com.openforum.rest.auth;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        // In a real scenario, we would verify the JWT signature here.
        // For this MVP/Multipass, we assume the gateway/parent has verified it.
        // We decode the payload to get user info.

        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                filterChain.doFilter(request, response);
                return;
            }
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8);

            // Simple parsing (replace with Jackson if needed, but keeping it minimal for
            // now)
            // Assuming payload is JSON: {"sub":"ext-123", "email":"user@example.com",
            // "name":"John Doe"}
            String externalId = extractJsonValue(payload, "sub");
            String email = extractJsonValue(payload, "email");
            String name = extractJsonValue(payload, "name");

            if (externalId != null) {
                Member member = memberRepository.findByExternalId(externalId)
                        .orElseGet(() -> {
                            Member newMember = Member.create(externalId, email, name, false);
                            memberRepository.save(newMember);
                            return newMember;
                        });

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        member, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log error, but don't fail hard, just don't authenticate
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1)
            return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;
        return json.substring(start, end);
    }
}
