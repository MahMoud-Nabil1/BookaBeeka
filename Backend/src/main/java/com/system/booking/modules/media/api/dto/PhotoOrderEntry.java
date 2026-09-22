package com.system.booking.modules.media.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PhotoOrderEntry(
        @NotNull UUID photoId,
        @NotNull Integer sortOrder
) {}
