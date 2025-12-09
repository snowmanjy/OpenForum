package com.openforum.rest.filter;

import com.openforum.domain.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Ensure this runs early to set up context and wraps everything for cleanup
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Priority 1: Auth Token (Handled by Security Chain later, will overwrite this)
            // Priority 2: Header (Handled here)
            String tenantId = request.getHeader(TENANT_HEADER);

            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenantId(tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Ensure ThreadLocal is cleared
            TenantContext.clear();
        }
    }
}
