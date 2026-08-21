package com.system.booking.modules.tenant.api;

import com.system.booking.modules.tenant.api.dto.TenantDto;
import com.system.booking.modules.tenant.api.dto.UpdateTenantRequestDto;
import com.system.booking.modules.tenant.dto.request.CreateTenantRequest;
import com.system.booking.modules.tenant.dto.response.TenantRegistrationResponse;
import com.system.booking.modules.tenant.internal.service.TenantRegistrationService;
import com.system.booking.modules.tenant.internal.service.TenantService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tenant management operations.
 *
 * <p>Includes public subdomain lookups, tenant owner profile management,
 * and SuperAdmin tenant onboarding.</p>
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantRegistrationService tenantRegistrationService;

    // -------------------------------------------------------------------------
    // GET /api/tenants/me
    // -------------------------------------------------------------------------

    /**
     * Returns the full profile and settings of the currently authenticated
     * tenant owner's tenant.
     *
     * <p>The {@code tenantId} is extracted from the JWT claims.</p>
     *
     * @param claims the JWT claims injected by the security filter
     * @return 200 OK with {@link TenantDto}
     */
    // @PreAuthorize("hasRole('OWNER')")  // TODO: uncomment after JWT filter is wired
    @GetMapping("/me")
    public ResponseEntity<TenantDto> getMyTenant(@RequestAttribute("claims") Claims claims) {
        UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
        return ResponseEntity.ok(tenantService.getTenantById(tenantId));
    }

    // -------------------------------------------------------------------------
    // PUT /api/tenants/me
    // -------------------------------------------------------------------------

    /**
     * Updates the authenticated tenant owner's business name, timezone,
     * currency, and/or custom settings.
     *
     * @param claims  the JWT claims injected by the security filter
     * @param request body containing the fields to update (all optional)
     * @return 200 OK with the updated {@link TenantDto}
     */
    // @PreAuthorize("hasRole('OWNER')")  // TODO: uncomment after JWT filter is wired
    @PutMapping("/me")
    public ResponseEntity<TenantDto> updateMyTenant(
            @RequestAttribute("claims") Claims claims,
            @RequestBody UpdateTenantRequestDto request) {
        UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
        return ResponseEntity.ok(tenantService.updateTenant(tenantId, request));
    }

    // -------------------------------------------------------------------------
    // GET /api/tenants/subdomain/{subdomain}
    // -------------------------------------------------------------------------

    /**
     * Public endpoint — resolves a subdomain to its tenant profile.
     *
     * <p>The frontend calls this when a customer visits
     * {@code tenantName.bookabeeka.com} to load tenant-specific branding,
     * timezone, and settings before the user logs in.</p>
     *
     * @param subdomain the tenant's unique subdomain slug
     * @return 200 OK with {@link TenantDto}
     */
    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<TenantDto> getTenantBySubdomain(
            @PathVariable String subdomain) {
        return ResponseEntity.ok(tenantService.getTenantBySubdomain(subdomain));
    }

    // -------------------------------------------------------------------------
    // POST /api/tenants/register
    // -------------------------------------------------------------------------

    /**
     * Onboards a new tenant by atomically creating a Tenant, a default Branch,
     * and an OWNER staff member.
     *
     * <p><b>Access:</b> SUPER_ADMIN only (defense-in-depth with both URL-level
     * and method-level authorization).</p>
     *
     * @param request validated onboarding data (tenant + branch + owner info)
     * @return 201 Created with the registration confirmation
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantRegistrationResponse> registerTenant(
            @Valid @RequestBody CreateTenantRequest request
    ) {
        TenantRegistrationResponse response = tenantRegistrationService.registerTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
