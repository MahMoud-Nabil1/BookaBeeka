package com.system.booking.modules.security.dto.response;

/**
 * Unified login response returned by both Staff and Customer authentication endpoints.
 *
 * <p>Contains the signed JWT token and a {@code userType} discriminator ("STAFF" or "CUSTOMER")
 * so the client application can route to the appropriate dashboard or storefront.</p>
 */
public record LoginResponse(
        String token,
        String userType
) {}