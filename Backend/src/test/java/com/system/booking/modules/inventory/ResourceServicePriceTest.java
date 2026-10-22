package com.system.booking.modules.inventory;

import com.system.booking.modules.inventory.internal.dto.request.CreateResourceRequest;
import com.system.booking.modules.inventory.internal.dto.request.UpdateResourceRequest;
import com.system.booking.modules.inventory.internal.dto.response.ResourceResponse;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.entity.RoomStatus;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
import com.system.booking.modules.inventory.internal.repository.RoomTypeRepository;
import com.system.booking.modules.inventory.internal.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServicePriceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private ResourceService resourceService;

    private UUID tenantId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        resourceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createResource extracts pricePerNight from specs when top-level price is null")
    void createResource_extractsPriceFromSpecs() {
        CreateResourceRequest req = new CreateResourceRequest(
                null,
                "Deluxe Ocean View",
                "101",
                1,
                RoomStatus.AVAILABLE,
                "ROOM",
                2,
                Map.of("pricePerNight", 500, "bedType", "KING"),
                null,
                "USD"
        );

        when(resourceRepository.existsByTenantIdAndRoomNumber(tenantId, "101")).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource saved = invocation.getArgument(0);
            saved.setId(resourceId);
            return saved;
        });

        ResourceResponse res = resourceService.createResource(tenantId, req);

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(captor.capture());
        Resource savedResource = captor.getValue();

        assertThat(savedResource.getPricePerNight()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(res.pricePerNight()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(((Number) res.specs().get("pricePerNight")).doubleValue()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("updateResource updates pricePerNight when provided in specs")
    void updateResource_updatesPriceFromSpecs() {
        Resource existing = Resource.builder()
                .tenantId(tenantId)
                .name("Standard Room")
                .roomNumber("102")
                .resourceType("ROOM")
                .pricePerNight(BigDecimal.valueOf(150))
                .specs(Map.of("pricePerNight", 150))
                .build();
        existing.setId(resourceId);

        when(resourceRepository.findByTenantIdAndId(tenantId, resourceId)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateResourceRequest updateReq = new UpdateResourceRequest(
                null,
                "Standard Room Updated",
                "102",
                null,
                null,
                null,
                null,
                Map.of("pricePerNight", 500),
                true,
                true,
                null,
                "USD"
        );

        ResourceResponse res = resourceService.updateResource(tenantId, resourceId, updateReq);

        assertThat(existing.getPricePerNight()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(res.pricePerNight()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(((Number) res.specs().get("pricePerNight")).doubleValue()).isEqualTo(500.0);
    }
}
