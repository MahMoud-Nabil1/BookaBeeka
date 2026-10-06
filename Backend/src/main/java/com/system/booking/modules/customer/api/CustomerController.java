package com.system.booking.modules.customer.api;

import com.system.booking.modules.customer.internal.dto.CustomerProfileResponse;
import com.system.booking.modules.customer.internal.dto.CustomerProfileUpdateRequest;
import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.service.CustomerService;
import com.system.booking.modules.security.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing public Customer operations.
 *
 * <p>Endpoints in this controller are generally public (permitted in {@code SecurityConfig})
 * as they deal with onboarding new customers before they have a JWT.</p>
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Public endpoint for customer self-registration.
     *
     * <p><b>Access:</b> Public (permitted in {@code SecurityConfig}).</p>
     *
     * @param request the validated registration data (name, email, password, phone)
     * @return 200 OK on successful registration
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody CustomerRegisterRequest request) {
        customerService.registerCustomer(request);
        return ResponseEntity.ok("Customer registered successfully");
    }

    /**
     * Get the authenticated customer's profile.
     *
     * <p><b>Access:</b> Authenticated CUSTOMER users only.</p>
     *
     * @return the customer profile response
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        UUID customerId = SecurityUtil.getCurrentCustomerPrincipal().id();
        CustomerProfileResponse profile = customerService.getCustomerProfile(customerId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the authenticated customer's profile.
     *
     * <p><b>Access:</b> Authenticated CUSTOMER users only.</p>
     *
     * @param request the validated profile update request
     * @return the updated customer profile response
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        UUID customerId = SecurityUtil.getCurrentCustomerPrincipal().id();
        CustomerProfileResponse updatedProfile = customerService.updateCustomerProfile(customerId, request);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Change the authenticated customer's password.
     *
     * <p><b>Access:</b> Authenticated CUSTOMER users only.</p>
     *
     * @param request the validated change password request containing current and new password
     * @return 200 OK with success message
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @Valid @RequestBody com.system.booking.modules.customer.internal.dto.CustomerChangePasswordRequest request) {
        UUID customerId = SecurityUtil.getCurrentCustomerPrincipal().id();
        customerService.changePassword(customerId, request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }
}