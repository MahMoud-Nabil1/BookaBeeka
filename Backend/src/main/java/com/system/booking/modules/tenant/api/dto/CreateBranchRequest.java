package com.system.booking.modules.tenant.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreateBranchRequest(
    @NotBlank(message = "Branch name is required") String name,
    String address,
    Map<String, Object> settings
) {}
