package com.system.booking.modules.security.controller;

import com.system.booking.modules.security.dto.request.CustomerLoginRequest;
import com.system.booking.modules.security.dto.request.StaffLoginRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling authentication (login) for both Staff and Customer users.
 *
 * <p>Exposes two completely separate login endpoints to guarantee isolation between
 * the Staff/Tenant ecosystem and the Customer ecosystem:</p>
 * <ul>
 *   <li>{@code POST /api/auth/staff/login}    — Staff/Admin authentication</li>
 *   <li>{@code POST /api/auth/customer/login}  — Customer authentication</li>
 * </ul>
 *
 * <p>Both endpoints are public (permitted in {@code SecurityConfig}) and return a
 * signed JWT in the {@link LoginResponse}.</p>
 *
 * <p><b>Note:</b> ADMIN is not a separate user type — an Admin is simply a Staff member
 * with role=ADMIN. There is no separate login flow for Admins.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Authenticates a Staff or Admin user.
     *
     * <p>On success, returns a JWT containing tenant-scoped claims
     * ({@code tenant_id}, {@code branch_id}, {@code role}).</p>
     *
     * @param request validated login credentials (email + password)
     * @return 200 OK with JWT token and user type "STAFF"
     */
    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> staffLogin(@Valid @RequestBody StaffLoginRequest request) {
        LoginResponse response = authenticationService.authenticateStaff(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a Customer user.
     *
     * <p>On success, returns a lightweight JWT with no tenant affiliation,
     * guaranteeing isolation from the Staff/Tenant ecosystem.</p>
     *
     * @param request validated login credentials (email + password)
     * @return 200 OK with JWT token and user type "CUSTOMER"
     */
    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponse> customerLogin(@Valid @RequestBody CustomerLoginRequest request) {
        LoginResponse response = authenticationService.authenticateCustomer(request);
        return ResponseEntity.ok(response);
    }
}