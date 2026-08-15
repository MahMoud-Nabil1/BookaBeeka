package com.system.booking.modules.security.security;

import com.system.booking.modules.security.context.TenantContext;
import com.system.booking.modules.security.context.TenantContextHolder;
import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.StaffPrincipal;
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
            String subjectId = jwtService.extractUsername(jwt);
            Claims claims = jwtService.extractAllClaims(jwt);
            String userType = claims.get("user_type", String.class);

            if (subjectId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (UserTypes.STAFF.name().equals(userType)) {
                    UUID staffId = UUID.fromString(subjectId);

                    // Null-safe check to handle SUPER_ADMIN tokens without tenant context
                    String tenantIdClaim = claims.get("tenant_id", String.class);
                    String branchIdClaim = claims.get("branch_id", String.class);

                    UUID tenantId = (tenantIdClaim != null) ? UUID.fromString(tenantIdClaim) : null;
                    UUID branchId = (branchIdClaim != null) ? UUID.fromString(branchIdClaim) : null;
                    String role = claims.get("role", String.class);

                    StaffPrincipal principal = new StaffPrincipal(
                            staffId,
                            null,
                            role,
                            tenantId,
                            branchId
                    );

                    // Populate TenantContext only if tenantId is present
                    if (tenantId != null) {
                        TenantContextHolder.setContext(new TenantContext(tenantId, branchId));
                    }

                    // Format authority cleanly without duplicate ROLE_ prefixes
                    String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;

                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } else if (UserTypes.CUSTOMER.name().equals(userType)) {
                    UUID customerId = UUID.fromString(subjectId);

                    CustomerPrincipal principal = new CustomerPrincipal(
                            customerId,
                            null
                    );

                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}