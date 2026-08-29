package com.system.booking.modules.tenant.internal.service;

import com.system.booking.modules.tenant.api.TenantModuleApi;
import com.system.booking.modules.tenant.api.dto.BranchDto;
import com.system.booking.modules.tenant.api.dto.TenantDto;
import com.system.booking.modules.tenant.api.dto.UpdateTenantRequestDto;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core tenant business logic.
 *
 * <p>Implements {@link TenantModuleApi} so other modules can inject the
 * interface for cross-module tenant lookups.</p>
 */
@Service
@RequiredArgsConstructor
public class TenantService implements TenantModuleApi {

    private final TenantRepository tenantRepository;
    private final BranchService branchService;

    // -------------------------------------------------------------------------
    // TenantModuleApi implementation (cross-module contract)
    // -------------------------------------------------------------------------

    @Override
    public TenantDto getTenantById(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        return toDto(tenant);
    }

    @Override
    public TenantDto getTenantBySubdomain(String subdomain) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found for subdomain: " + subdomain));
        return toDto(tenant);
    }

    @Override
    public BranchDto getBranchById(UUID tenantId, UUID branchId) {
        return branchService.getBranchById(tenantId, branchId);
    }

    // -------------------------------------------------------------------------
    // Controller-facing methods
    // -------------------------------------------------------------------------

    /**
     * Updates a tenant's profile fields. Only non-null values are applied.
     *
     * @param tenantId the tenant's UUID (from JWT)
     * @param request  the fields to update
     * @return the updated tenant DTO
     */
    @Transactional
    public TenantDto updateTenant(UUID tenantId, UpdateTenantRequestDto request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.settings() != null) {
            tenant.setSettings(request.settings());
        }
        if (request.timezone() != null) {
            tenant.setTimezone(request.timezone());
        }
        if (request.currency() != null) {
            tenant.setCurrency(request.currency());
        }

        Tenant saved = tenantRepository.save(tenant);
        return toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------------

    private TenantDto toDto(Tenant tenant) {
        return new TenantDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getStatus(),
                tenant.getSettings(),
                tenant.getTimezone(),
                tenant.getCurrency(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
