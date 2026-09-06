package com.system.booking.modules.security.service;

import com.system.booking.modules.security.dto.AuthUserDTO;
import com.system.booking.modules.security.dto.request.LoginRequest;
import com.system.booking.modules.security.dto.response.LoginResponse;
import com.system.booking.modules.security.port.in.CustomerAuthPort;
import com.system.booking.modules.security.port.in.HotelAdminAuthPort;
import com.system.booking.modules.security.port.in.OwnerAuthPort;
import com.system.booking.modules.security.port.in.SuperAdminAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final SuperAdminAuthPort superAdminPort;
    private final OwnerAuthPort ownerPort;
    private final HotelAdminAuthPort hotelAdminPort;
    private final CustomerAuthPort customerPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse loginSuperAdmin(LoginRequest request) {
        var user = superAdminPort.findSuperAdminByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginOwner(LoginRequest request) {
        var user = ownerPort.findOwnerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginAdmin(LoginRequest request) {
        var user = hotelAdminPort.findAdminByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    public LoginResponse loginCustomer(LoginRequest request) {
        var user = customerPort.findCustomerByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return verifyAndIssueToken(user, request.password());
    }

    private LoginResponse verifyAndIssueToken(AuthUserDTO user, String rawPassword) {
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.role());
    }
}