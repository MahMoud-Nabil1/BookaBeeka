package com.system.booking.modules.owner.api.dto;

import java.util.Map;

/**
 * Request payload for editing branch details.
 *
 * <p>All fields are optional — only non-null fields are applied.</p>
 */
public record EditBranchRequest(
        String name,
        String address,
        /** Branch status: ACTIVE, INACTIVE, CLOSED, MAINTENANCE. */
        String status,
        Map<String, Object> settings
) {}
