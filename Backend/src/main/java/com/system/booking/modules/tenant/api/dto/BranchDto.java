package com.system.booking.modules.tenant.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record BranchDto(
    UUID id,
    UUID tenantId,
    String name,
    String address,
    String status,
    Map<String, Object> settings,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
