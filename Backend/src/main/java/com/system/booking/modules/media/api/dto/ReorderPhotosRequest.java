package com.system.booking.modules.media.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ReorderPhotosRequest(
        List<PhotoOrderEntry> photos,
        List<UUID> photoIds
) {
    public ReorderPhotosRequest(List<PhotoOrderEntry> photos) {
        this(photos, null);
    }

    public List<PhotoOrderEntry> getEffectiveEntries() {
        if (photos != null && !photos.isEmpty()) {
            return photos;
        }
        if (photoIds != null && !photoIds.isEmpty()) {
            List<PhotoOrderEntry> entries = new ArrayList<>();
            for (int i = 0; i < photoIds.size(); i++) {
                entries.add(new PhotoOrderEntry(photoIds.get(i), i));
            }
            return entries;
        }
        return List.of();
    }
}
