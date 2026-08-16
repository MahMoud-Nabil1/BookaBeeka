package com.system.booking.modules.security.util;

import com.system.booking.modules.security.model.principal.CustomerPrincipal;
import com.system.booking.modules.security.model.principal.StaffPrincipal;
import com.system.booking.modules.security.security.UserTypes;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
        // Utility class — prevent instantiation
    }

    public static StaffPrincipal getCurrentStaffPrincipal() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof StaffPrincipal staffPrincipal) {
            return staffPrincipal;
        }

        throw new IllegalStateException("Expected StaffPrincipal but found: " + getPrincipalClassName(principal));
    }

    public static CustomerPrincipal getCurrentCustomerPrincipal() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomerPrincipal customerPrincipal) {
            return customerPrincipal;
        }

        throw new IllegalStateException("Expected CustomerPrincipal but found: " + getPrincipalClassName(principal));
    }

    public static String getCurrentUserType() {
        Authentication authentication = getRequiredAuthentication();
        Object principal = authentication.getPrincipal();

        return switch (principal) {
            case StaffPrincipal ignored -> UserTypes.STAFF.name();
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