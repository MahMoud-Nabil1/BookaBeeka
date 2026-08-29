package com.system.booking.modules.tenant.api;

import com.system.booking.modules.tenant.api.dto.BranchDto;
import com.system.booking.modules.tenant.api.dto.TenantDto;

import java.util.UUID;

/**
 * Public contract for the Tenant module.
 *
 * <p>Other modules (booking, payment, admin, etc.) should inject this
 * interface to access tenant data instead of reaching into the tenant
 * repository or entity layer directly.</p>
 */
public interface TenantModuleApi {

    /**
     * Retrieves a tenant by its ID.
     *
     * @param tenantId UUID of the tenant
     * @return the tenant DTO
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    TenantDto getTenantById(UUID tenantId);

    /**
     * Retrieves a tenant by its unique subdomain.
     *
     * @param subdomain the tenant's subdomain (e.g. "salon-elite")
     * @return the tenant DTO
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    TenantDto getTenantBySubdomain(String subdomain);

    BranchDto getBranchById(UUID tenantId, UUID branchId);
}
