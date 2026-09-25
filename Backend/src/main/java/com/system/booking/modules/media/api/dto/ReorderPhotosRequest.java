package com.system.booking.modules.media.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderPhotosRequest(
        @NotEmpty @Valid List<PhotoOrderEntry> photos
) {}
