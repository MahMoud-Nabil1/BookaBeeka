package com.system.booking.modules.tenant.internal.service;

import com.system.booking.common.config.CacheConfig;
import com.system.booking.modules.tenant.api.TenantModuleApi;
import com.system.booking.modules.tenant.internal.dto.TenantDto;
import com.system.booking.modules.tenant.internal.dto.UpdateTenantRequestDto;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService implements TenantModuleApi {

    private final TenantRepository tenantRepository;

    /**
     * Looks up a tenant by its primary key.
     * Cached for 30 minutes — this is called on virtually every authenticated request
     * (booking, payment, inventory) to resolve the tenant context from the JWT tenantId claim.
     */
    @Override
    @Cacheable(value = CacheConfig.CACHE_TENANTS, key = "#tenantId", unless = "#result == null")
    public TenantDto getTenantById(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        return toDto(tenant);
    }

    /**
     * Looks up a tenant by subdomain — called on every public availability search.
     * Cached for 30 minutes; evicted when the tenant profile is updated.
     */
    @Override
    @Cacheable(value = CacheConfig.CACHE_TENANT_SUBDOMAIN, key = "#subdomain", unless = "#result == null")
    public TenantDto getTenantBySubdomain(String subdomain) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found for subdomain: " + subdomain));
        return toDto(tenant);
    }

    /**
     * Updates tenant profile. Evicts both tenant caches so the next read is fresh.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_TENANTS, key = "#tenantId"),
            @CacheEvict(value = CacheConfig.CACHE_TENANT_SUBDOMAIN, allEntries = true)
    })
    public TenantDto updateTenant(UUID tenantId, UpdateTenantRequestDto request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        if (request.hotelName() != null) {
            tenant.setName(request.hotelName());
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