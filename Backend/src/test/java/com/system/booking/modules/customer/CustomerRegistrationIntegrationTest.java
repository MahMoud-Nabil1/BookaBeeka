package com.system.booking.modules.customer;

import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.customer.internal.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterCustomerSuccessfully() throws Exception {

        String requestBody = """
                {
                    "firstName": "Ahmed",
                    "lastName": "Test",
                    "email": "ahmed.test@example.com",
                    "password": "123456",
                    "phone": "01000000000"
                }
                """;

        mockMvc.perform(
                        post("/api/customers/register")
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        Customer customer = customerRepository
                .findByEmail("ahmed.test@example.com")
                .orElseThrow();

        // Verify customer data
        assert customer.getFirstName().equals("Ahmed");
        assert customer.getLastName().equals("Test");
        assert customer.getEmail().equals("ahmed.test@example.com");

        // Verify password was hashed
        assert !customer.getPasswordHash().equals("123456");

        // Verify the stored hash matches the original password
        assert passwordEncoder.matches(
                "123456",
                customer.getPasswordHash()
        );
    }
    @Test
    void shouldRejectDuplicateCustomerEmail() throws Exception {

        // First registration
        String firstRequest = """
            {
                "firstName": "Ahmed",
                "lastName": "Test",
                "email": "duplicate@example.com",
                "password": "123456",
                "phone": "01000000000"
            }
            """;

        mockMvc.perform(
                        post("/api/customers/register")
                                .contentType(APPLICATION_JSON)
                                .content(firstRequest)
                )
                .andExpect(status().isOk());


        // Second registration using the same email
        String duplicateRequest = """
            {
                "firstName": "Another",
                "lastName": "User",
                "email": "duplicate@example.com",
                "password": "654321",
                "phone": "01111111111"
            }
            """;

        mockMvc.perform(
                        post("/api/customers/register")
                                .contentType(APPLICATION_JSON)
                                .content(duplicateRequest)
                )
                .andExpect(status().isUnprocessableEntity());


        // Make sure only ONE customer exists with this email
        long count = customerRepository.findAll()
                .stream()
                .filter(customer ->
                        customer.getEmail().equals("duplicate@example.com"))
                .count();

        assert count == 1;
    }
    @Test
    void shouldRejectInvalidCustomerData() throws Exception {

        String invalidRequest = """
            {
                "firstName": "Ahmed",
                "lastName": "Test",
                "email": "invalid-email",
                "password": "123",
                "phone": "01000000000"
            }
            """;

        mockMvc.perform(
                        post("/api/customers/register")
                                .contentType(APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());
    }
}
