package com.system.booking.modules.security.controller;

import com.system.booking.modules.security.dto.request.LoginRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller — Role-Segregated Login Endpoints.
 *
 * <p><b>Architectural Decision — Why separate endpoints per role?</b></p>
 * <p>Instead of a single generic {@code /login} endpoint that accepts a role flag in
 * the request body, authentication is split into dedicated per-role endpoints. This
 * design provides several guarantees:</p>
 * <ol>
 *   <li><b>JWT claim correctness:</b> Each login method delegates to a role-specific
 *       auth port (e.g., {@code OwnerAuthPort}), which is the only source of truth for
 *       that user type. This prevents cross-role credential reuse — an Owner's credentials
 *       cannot be submitted to the SuperAdmin endpoint and vice versa, because each endpoint
 *       queries a separate user table.</li>
 *   <li><b>Tenant isolation at token issuance:</b> The Owner and Admin login paths embed
 *       the caller's {@code tenant_id} directly into the JWT. This claim is the sole
 *       mechanism for multi-tenant data scoping throughout the inventory and booking
 *       modules — it is never supplied by the client at runtime.</li>
 *   <li><b>Auditability:</b> Separate endpoints make it trivial to add role-specific
 *       rate limiting, logging, or MFA requirements in the future without touching the
 *       logic of other roles.</li>
 * </ol>
 *
 * <p>All endpoints are publicly accessible (permitted in {@code SecurityConfig}) because
 * authentication must precede authorization.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * SuperAdmin login endpoint.
     *
     * <p>Authenticates against the {@code super_admin} user table. The resulting JWT
     * contains {@code role=SUPER_ADMIN} with a {@code null} tenant_id, granting
     * platform-wide access without being scoped to any hotel tenant.</p>
     */
    @PostMapping("/super-admin/login")
    public ResponseEntity<LoginResponse> superAdminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.loginSuperAdmin(request));
    }

    /**
     * Hotel Owner login endpoint.
     *
     * <p>Authenticates against the {@code hotel_owner} table. The resulting JWT embeds
     * the Owner's {@code tenant_id} claim, which is automatically extracted by the
     * {@code JwtAuthenticationFilter} and stored in the {@code TenantContextHolder}
     * for every subsequent request. This is the cornerstone of tenant isolation — the
     * tenant scope is established once at login and never relies on client-supplied IDs.</p>
     */
    @PostMapping("/owner/login")
    public ResponseEntity<LoginResponse> ownerLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.loginOwner(request));
    }

    /**
     * Hotel Admin login endpoint.
     *
     * <p>Authenticates against the {@code hotel_admin} table. Like the Owner token,
     * the Admin JWT carries a {@code tenant_id} claim, restricting all inventory
     * and availability operations to the Admin's assigned hotel tenant.</p>
     */
    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.loginAdmin(request));
    }

    /**
     * Customer login endpoint.
     *
     * <p>Authenticates against the {@code customer} table. Customer JWTs carry
     * {@code role=CUSTOMER} with no {@code tenant_id}, reflecting the fact that
     * customers browse across all hotels and are not scoped to any single tenant.</p>
     */
    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponse> customerLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.loginCustomer(request));
    }
}