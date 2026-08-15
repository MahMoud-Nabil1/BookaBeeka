package com.system.booking.modules.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the tenant onboarding (registration) endpoint.
 *
 * <p>This DTO captures all data needed to atomically create a complete tenant
 * ecosystem in a single API call:</p>
 * <ol>
 *   <li><b>Tenant</b> — the business entity (e.g., a clinic or salon)</li>
 *   <li><b>Default Branch</b> — the first (main) branch of the business</li>
 *   <li><b>Owner Staff</b> — the business owner with full tenant access (role=OWNER)</li>
 * </ol>
 *
 * <p><b>Access Control:</b> Only a SUPER_ADMIN can submit this request via
 * {@code POST /api/tenants/register}.</p>
 */
public record CreateTenantRequest(

        // ── 1. Tenant Data ──────────────────────────────────────────────────────

        @NotBlank(message = "Tenant name is required")
        String tenantName,

        @NotBlank(message = "Subdomain is required")
        @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
        String subdomain,

        // ── 2. Default Branch Data ──────────────────────────────────────────────

        @NotBlank(message = "Branch name is required")
        String branchName,

        /** Branch address — optional during initial onboarding. */
        String branchAddress,

        // ── 3. Owner (Business Owner) Data ──────────────────────────────────────

        @NotBlank(message = "Owner first name is required")
        String ownerFirstName,

        @NotBlank(message = "Owner last name is required")
        String ownerLastName,

        @NotBlank(message = "Owner email is required")
        @Email(message = "Invalid email format")
        String ownerEmail,

        @NotBlank(message = "Owner password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String ownerPassword,

        /** Owner phone number — optional during initial onboarding. */
        String ownerPhone
) {}