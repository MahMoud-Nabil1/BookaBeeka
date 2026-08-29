package com.system.booking.modules.tenant.api.dto;

import java.util.Map;

public record UpdateBranchRequest(
    String name,
    String address,
    Map<String, Object> settings
) {}
