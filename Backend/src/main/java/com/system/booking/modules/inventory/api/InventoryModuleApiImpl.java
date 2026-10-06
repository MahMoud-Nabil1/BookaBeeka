package com.system.booking.modules.inventory.api;

import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;
import com.system.booking.modules.inventory.internal.service.AmenityService;
import com.system.booking.modules.inventory.internal.service.ResourceService;
import com.system.booking.modules.inventory.internal.service.RoomTypeService;
import com.system.booking.modules.inventory.internal.service.ServiceOfferingService;
import com.system.booking.modules.inventory.internal.repository.ResourceAmenityRepository;
import com.system.booking.modules.inventory.internal.repository.ResourceServiceLinkRepository;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryModuleApiImpl implements InventoryModuleApi {

    private final ResourceService resourceService;
    private final ServiceOfferingService serviceOfferingService;
    private final RoomTypeService roomTypeService;
    private final ResourceAmenityRepository resourceAmenityRepository;
    private final ResourceServiceLinkRepository resourceServiceLinkRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;

    @Override
    public List<String> listAmenityNamesForResource(UUID resourceId) {
        return resourceAmenityRepository.findByResourceId(resourceId).stream()
                .map(link -> link.getAmenity().getName())
                .collect(Collectors.toList());
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

    @Override
    public RoomTypeResponse getRoomTypeByTenantAndId(UUID tenantId, UUID roomTypeId) {
        return roomTypeService.getRoomType(tenantId, roomTypeId);
    }

    @Override
    public UUID getFirstServiceOfferingIdForRoom(UUID tenantId, UUID roomId) {
        return resourceServiceLinkRepository.findByTenantIdAndResourceId(tenantId, roomId)
                .stream()
                .findFirst()
                .map(link -> link.getServiceOffering().getId())
                .orElse(null);
    }

    @Override
    public boolean serviceOfferingExists(UUID serviceOfferingId) {
        return serviceOfferingId != null && serviceOfferingRepository.existsById(serviceOfferingId);
    }
}