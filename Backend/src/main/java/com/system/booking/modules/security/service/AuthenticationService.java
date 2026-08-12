package com.system.booking.modules.security.service;

import com.system.booking.modules.security.dto.request.CustomerLoginRequest;
import com.system.booking.modules.security.dto.request.StaffLoginRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.port.in.CustomerAuthenticationPort;
import com.system.booking.modules.security.port.in.StaffAuthenticationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final StaffAuthenticationPort staffPort;
    private final CustomerAuthenticationPort customerPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse authenticateStaff(StaffLoginRequest request) {

        var staff = staffPort.findStaffByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!staff.isActive()) {
            throw new BadCredentialsException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), staff.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        var token = jwtService.generateStaffToken(staff);
        return new LoginResponse(token, "STAFF");
    }

    public LoginResponse authenticateCustomer(CustomerLoginRequest request) {

        var customer = customerPort.findCustomerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), customer.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        var token = jwtService.generateCustomerToken(customer);
        return new LoginResponse(token, "CUSTOMER");
    }
}