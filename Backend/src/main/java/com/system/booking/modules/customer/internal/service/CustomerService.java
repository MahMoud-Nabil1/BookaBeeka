package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.customer.internal.dto.CustomerRegisterRequest;
import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    // السر كله هنا! بنستدعي مكنة التشفير اللي بنيناها في السكيورتي
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerCustomer(CustomerRegisterRequest request) {
        // 1. نتأكد إن الإيميل مش متسجل قبل كده
        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        // 2. ننشئ العميل الجديد
        Customer customer = new Customer();
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        // 3. التشفير! (بناخد الباسورد العادي، نشفره، ونحفظ الهاش)
        String hashedPass = passwordEncoder.encode(request.password());
        customer.setPasswordHash(hashedPass);

        // 4. نحفظ في الداتا بيز
        customerRepository.save(customer);
    }
}