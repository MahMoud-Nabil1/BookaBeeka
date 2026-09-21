package com.system.booking.modules.security.util;

import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.HotelUserPrincipal;
import com.system.booking.modules.security.security.UserTypes;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtil {

    private SecurityUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Retrieves the authenticated HotelUserPrincipal (SuperAdmin, Owner, or Admin).
     */
    public static HotelUserPrincipal getCurrentHotelUserPrincipal() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof HotelUserPrincipal hotelUserPrincipal) {
            return hotelUserPrincipal;
        }

        throw new IllegalStateException("Expected HotelUserPrincipal but found: " + getPrincipalClassName(principal));
    }

    /**
     * Retrieves the authenticated CustomerPrincipal.
     */
    public static CustomerPrincipal getCurrentCustomerPrincipal() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomerPrincipal customerPrincipal) {
            return customerPrincipal;
        }

        throw new IllegalStateException("Expected CustomerPrincipal but found: " + getPrincipalClassName(principal));
    }

    /**
     * Helper to quickly get the current tenant ID from the HotelUserPrincipal.
     */
    public static UUID getCurrentTenantId() {
        return getCurrentHotelUserPrincipal().tenantId();
    }

    /**
     * Resolves the current user type (HOTEL_USER or CUSTOMER).
     */
    public static String getCurrentUserType() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        return switch (principal) {
            case HotelUserPrincipal ignored -> UserTypes.HOTEL_USER.name();
            case CustomerPrincipal ignored -> UserTypes.CUSTOMER.name();
            default -> throw new IllegalStateException("Unknown principal type: " + getPrincipalClassName(principal));
        };
    }

    private static String getPrincipalClassName(Object principal) {
        return (principal != null) ? principal.getClass().getSimpleName() : "null";
    }

    private static Authentication getRequiredAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("User is anonymous and not fully authenticated");
        }

        return authentication;
    }
}