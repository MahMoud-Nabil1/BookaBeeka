package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.ResourceResponse;
import com.system.booking.modules.inventory.internal.dto.ServiceOfferingResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryModuleApi {
    ServiceOfferingResponse getServiceOfferingByTenantAndId(UUID tenantId, UUID serviceOfferingId);
    ResourceResponse getResourceByTenantAndId(UUID tenantId, UUID resourceId);
    List<ResourceResponse> listResourcesForTenant(UUID tenantId);
    List<String> listAmenityNamesForResource(UUID resourceId);
}
