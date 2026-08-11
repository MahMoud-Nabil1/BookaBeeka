package com.system.booking.modules.customer.internal.service;

import com.system.booking.modules.security.dto.CustomerAuthDTO;
import com.system.booking.modules.security.port.in.CustomerAuthenticationPort;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerSecurityAdapter implements CustomerAuthenticationPort {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAuthDTO> findCustomerByEmail(String email) {
        // 1. بندور على العميل في الداتا بيز
        return customerRepository.findByEmail(email)
                .map(customer -> new CustomerAuthDTO(
                        customer.getId(), // موروث من BaseEntity
                        customer.getEmail(),
                        customer.getPasswordHash()
                ));
    }
}