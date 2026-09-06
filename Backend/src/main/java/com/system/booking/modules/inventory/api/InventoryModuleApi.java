package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryModuleApi {
    ServiceOfferingResponse getServiceOfferingByTenantAndId(UUID tenantId, UUID serviceOfferingId);
    ResourceResponse getResourceByTenantAndId(UUID tenantId, UUID resourceId);
    List<ResourceResponse> listResourcesForTenant(UUID tenantId);
    List<String> listAmenityNamesForResource(UUID resourceId);
    RoomTypeResponse getRoomTypeByTenantAndId(UUID tenantId, UUID roomTypeId);
}