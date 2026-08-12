package com.system.booking.modules.security.config;

import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // الكود ده هيشتغل مرة واحدة بس أول ما البروجكت يقوم
    @Bean
    public CommandLineRunner testDataSeeder(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String testEmail = "test@customer.com";
            if (customerRepository.findByEmail(testEmail).isEmpty()) {
                Customer customer = new Customer();
                customer.setEmail(testEmail);
                customer.setPasswordHash(passwordEncoder.encode("123456")); // هنا بيتشفر صح!
                customer.setFirstName("Test");
                customer.setLastName("Customer");
                customer.setPhone("01000000000");

                customerRepository.save(customer);
                System.out.println("✅ Test Customer Created Successfully!");
            }
        };
    }
}