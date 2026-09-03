package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.ResourceResponse;
import com.system.booking.modules.inventory.internal.dto.ServiceOfferingResponse;
import com.system.booking.modules.inventory.internal.service.AmenityService;
import com.system.booking.modules.inventory.internal.service.ResourceService;
import com.system.booking.modules.inventory.internal.service.ServiceOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryModuleApiImpl implements InventoryModuleApi {
    
    private final ResourceService resourceService;
    private final ServiceOfferingService serviceOfferingService;
    private final AmenityService amenityService;

    @Override
    public List<String> listAmenityNamesForResource(UUID resourceId) {
        return amenityService.listAmenitiesForResource(resourceId).stream()
                .map(a -> a.name())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public ServiceOfferingResponse getServiceOfferingByTenantAndId(UUID tenantId, UUID serviceOfferingId) {
        return serviceOfferingService.getServiceOffering(tenantId, serviceOfferingId);
    }

    @Override
    public ResourceResponse getResourceByTenantAndId(UUID tenantId, UUID resourceId) {
        return resourceService.getResource(tenantId, resourceId);
    }

    @Override
    public List<ResourceResponse> listResourcesForTenant(UUID tenantId) {
        return resourceService.listResources(tenantId);
    }
}
