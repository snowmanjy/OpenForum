package com.openforum.rest.security;

import com.openforum.domain.aggregate.Member;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityContext {

    public static UUID getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Member) {
            return ((Member) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
