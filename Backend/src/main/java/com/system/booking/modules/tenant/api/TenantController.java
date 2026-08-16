package com.system.booking.modules.tenant.api;

import com.system.booking.modules.tenant.dto.request.CreateTenantRequest;
import com.system.booking.modules.tenant.dto.response.TenantRegistrationResponse;
import com.system.booking.modules.tenant.internal.service.TenantRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant management operations.
 *
 * <p>Currently exposes a single endpoint for tenant onboarding, restricted
 * to platform-level SUPER_ADMIN users. As the system evolves, additional
 * tenant management endpoints (update settings, suspend, etc.) will be
 * added to this controller.</p>
 *
 * <p><b>Security:</b> The {@code /api/tenants/register} endpoint is protected
 * at two levels:</p>
 * <ol>
 *   <li><b>URL-level</b> — {@code SecurityConfig} restricts it to {@code ROLE_SUPER_ADMIN}</li>
 *   <li><b>Method-level</b> — {@code @PreAuthorize} provides defense-in-depth</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRegistrationService tenantRegistrationService;

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
