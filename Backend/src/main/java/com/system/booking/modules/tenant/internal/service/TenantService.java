package com.system.booking.modules.tenant.internal.service;

import com.system.booking.modules.tenant.api.TenantModuleApi;
import com.system.booking.modules.tenant.internal.dto.TenantDto;
import com.system.booking.modules.tenant.internal.dto.UpdateTenantRequestDto;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService implements TenantModuleApi {

    private final TenantRepository tenantRepository;

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