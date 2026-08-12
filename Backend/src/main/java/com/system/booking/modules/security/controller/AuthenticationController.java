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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // مسار اللوجن للموظفين
    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponse> staffLogin(@Valid @RequestBody StaffLoginRequest request) {
        LoginResponse response = authenticationService.authenticateStaff(request);
        return ResponseEntity.ok(response);
    }

    // مسار اللوجن للعملاء
    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponse> customerLogin(@Valid @RequestBody CustomerLoginRequest request) {
        LoginResponse response = authenticationService.authenticateCustomer(request);
        return ResponseEntity.ok(response);
    }
}