package com.system.booking.modules.tenant.api;

import com.system.booking.modules.tenant.internal.dto.TenantDto;

import java.util.UUID;

public interface TenantModuleApi {

    TenantDto getTenantById(UUID tenantId);

    TenantDto getTenantBySubdomain(String subdomain);
}