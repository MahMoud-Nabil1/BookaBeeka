package com.system.booking.modules.inventory.internal.service;

import com.system.booking.common.config.CacheConfig;
import com.system.booking.modules.inventory.internal.dto.response.RoomTypeResponse;
import com.system.booking.modules.inventory.internal.entity.RoomType;
import com.system.booking.modules.inventory.internal.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies that {@link RoomTypeService} correctly caches list results and
 * evicts the cache on writes.
 *
 * <p>Uses @MockitoSpyBean (Spring Boot 3.4+) to wrap the real repository
 * in a spy so we can verify how many times the DB was actually hit.</p>
 *
 * <p><b>Note on H2:</b> Tests use H2 in-memory database (test scope in pom.xml).
 * The cache beans are still fully wired, so Caffeine behaviour is real.</p>
 */
@SpringBootTest
class RoomTypeCacheTest {

    @MockitoSpyBean
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private CacheManager cacheManager;

    private final UUID tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void clearCache() {
        // Ensure each test starts with a cold cache
        var cache = cacheManager.getCache(CacheConfig.CACHE_ROOM_TYPES);
        if (cache != null) cache.clear();
    }

    @Test
    @DisplayName("listRoomTypes: DB is hit only once on repeated calls (cache hit on second call)")
    void listRoomTypes_shouldHitDatabaseOnlyOnce() {
        // Arrange
        RoomType rt = RoomType.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Deluxe")
                .isActive(true)
                .build();
        when(roomTypeRepository.findByTenantId(tenantId)).thenReturn(List.of(rt));

        // Act — call twice
        List<RoomTypeResponse> first  = roomTypeService.listRoomTypes(tenantId);
        List<RoomTypeResponse> second = roomTypeService.listRoomTypes(tenantId);

        // Assert — repository called exactly once; second result from cache
        verify(roomTypeRepository, times(1)).findByTenantId(tenantId);
        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
    }

    @Test
    @DisplayName("deleteRoomType: cache is evicted so the next listRoomTypes hits the DB again")
    void deleteRoomType_shouldEvictCache() {
        // Arrange
        UUID roomTypeId = UUID.randomUUID();
        RoomType rt = RoomType.builder()
                .id(roomTypeId)
                .tenantId(tenantId)
                .name("Standard")
                .isActive(true)
                .build();
        when(roomTypeRepository.findByTenantId(tenantId)).thenReturn(List.of(rt));
        when(roomTypeRepository.findByTenantIdAndId(tenantId, roomTypeId)).thenReturn(Optional.of(rt));

        // Warm the cache
        roomTypeService.listRoomTypes(tenantId);
        verify(roomTypeRepository, times(1)).findByTenantId(tenantId);

        // Act — evicting write
        roomTypeService.deleteRoomType(tenantId, roomTypeId);

        // After eviction the next list call must go to the DB again
        when(roomTypeRepository.findByTenantId(tenantId)).thenReturn(List.of());
        roomTypeService.listRoomTypes(tenantId);

        verify(roomTypeRepository, times(2)).findByTenantId(tenantId);
    }
}
