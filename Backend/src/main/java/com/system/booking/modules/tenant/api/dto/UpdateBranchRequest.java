package com.system.booking.modules.tenant.api.dto;

import java.util.Map;

/**
 * Request payload for updating a branch's details.
 *
 * <p>All fields are optional — only non-null fields are applied.</p>
 */
public record UpdateBranchRequest(
    String name,
    String address,
    /** Branch status: ACTIVE, INACTIVE, CLOSED, MAINTENANCE. Null means no change. */
    String status,
    Map<String, Object> settings
) {}
