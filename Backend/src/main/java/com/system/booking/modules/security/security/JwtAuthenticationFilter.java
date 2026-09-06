package com.system.booking.modules.security.security;

import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT Authentication Filter — executed once per HTTP request.
 *
 * <p>This filter is the entry point for all stateless authentication. It validates
 * the incoming JWT, extracts role and tenant claims, builds the appropriate
 * {@link org.springframework.security.core.Authentication} principal, and — for
 * hotel staff (Owner / Admin / SuperAdmin) — populates the
 * {@link TenantContextHolder} with the authenticated tenant's ID so that
 * downstream inventory and booking services can enforce strict data isolation
 * without relying on user-supplied request parameters.</p>
 *
 * <p><b>Multi-Tenancy note:</b> The {@code tenant_id} claim is embedded in the
 * JWT at login time (see {@link com.system.booking.modules.security.service.JwtService}).
 * It is never taken from the request body — this eliminates any possibility of a
 * tenant impersonation attack via a crafted payload.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtService.extractAllClaims(jwt);
                UUID userId = jwtService.extractUserId(jwt);
                String role = claims.get("role", String.class);
                String tenantIdClaim = claims.get("tenant_id", String.class);
                UUID tenantId = (tenantIdClaim != null) ? UUID.fromString(tenantIdClaim) : null;

                String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;
                var authorities = Collections.singletonList(new SimpleGrantedAuthority(authority));

                if ("CUSTOMER".equalsIgnoreCase(role) || "ROLE_CUSTOMER".equalsIgnoreCase(role)) {
                    // Customers are not scoped to any tenant — they browse across all hotels.
                    // No TenantContext is set, which correctly prevents access to inventory endpoints.
                    CustomerPrincipal principal = new CustomerPrincipal(userId, null);
                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } else {
                    // Hotel staff roles: SUPER_ADMIN, OWNER, ADMIN.
                    // HotelUserPrincipal carries the tenantId for method-level security checks.
                    HotelUserPrincipal principal = new HotelUserPrincipal(userId, null, role, tenantId);

                    // Populate TenantContext only for users bound to a specific hotel tenant.
                    // SuperAdmins have a null tenantId and therefore receive no tenant scope,
                    // preventing them from accidentally leaking cross-tenant data in scoped queries.
                    if (tenantId != null) {
                        TenantContextHolder.setContext(new TenantContext(tenantId));
                    }

                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear TenantContext after the request completes, regardless of success or failure.
            // This is critical in thread-pool environments (e.g., Tomcat) where threads are reused:
            // a leaked ThreadLocal would cause the next request on the same thread to inherit a
            // stale tenant scope, resulting in cross-tenant data exposure.
            TenantContextHolder.clear();
        }
    }
}