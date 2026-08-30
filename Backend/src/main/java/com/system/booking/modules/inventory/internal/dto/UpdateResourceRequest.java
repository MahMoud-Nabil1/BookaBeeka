package com.system.booking.modules.inventory.internal.dto;

import java.util.Map;

public record UpdateResourceRequest(
    String name,
    String resourceType,
    Integer capacity,
    Map<String, Object> specs,
    Boolean isActive,
    Boolean isBookable
) {}
