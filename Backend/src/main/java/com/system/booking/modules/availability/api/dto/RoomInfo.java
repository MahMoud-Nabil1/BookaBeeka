package com.system.booking.modules.availability.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Carries room-level data in the availability search response.
 *
 * <p>{@code primaryPhotoUrl} is the Cloudinary {@code secure_url} of the room's
 * primary photo ({@code is_primary = true}), fetched in a single batch query alongside
 * the search results. It is {@code null} when no photo has been uploaded for the room —
 * the frontend should fall back to its stock-image logic in that case.</p>
 */
public record RoomInfo(
        UUID id,
        String name,
        String roomType,
        Integer capacity,
        String bedType,
        List<String> amenities,
        Map<String, Object> specs,
        String primaryPhotoUrl
) {}
