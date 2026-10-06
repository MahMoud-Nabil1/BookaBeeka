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

    /**
     * Returns the first active service offering linked to the given room, or {@code null} if
     * none exists. Used as a fallback when a booking was created without an explicit
     * serviceOfferingId (room-only bookings priced by night) so that reviews can still
     * satisfy the FK into the {@code service} table.
     */
    UUID getFirstServiceOfferingIdForRoom(UUID tenantId, UUID roomId);

    /**
     * Returns {@code true} if a service offering with the given ID exists in the {@code service}
     * table. Used to guard against stale/orphaned serviceOfferingId values on old bookings.
     */
    boolean serviceOfferingExists(UUID serviceOfferingId);
}