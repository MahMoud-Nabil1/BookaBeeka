package com.system.booking.modules.customer.api;

import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}