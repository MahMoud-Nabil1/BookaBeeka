package com.system.booking.modules.payment.api;

import com.system.booking.modules.customer.internal.entity.Customer;
import com.system.booking.modules.payment.internal.entity.CustomerWallet;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Developer-only seeding controller for the payment module.
 *
 * <p><strong>Purpose:</strong> Creates the prerequisite data (Customer + CustomerWallet)
 * needed to test the payment flow end-to-end without manually inserting SQL rows.</p>
 *
 * <p><strong>⚠️ Guarded, not just documented:</strong> restricted via {@code @Profile}
 * to non-production profiles so this unauthenticated, data-creating endpoint cannot be
 * reached in production even if someone forgets to remove it before deploy. The profile
 * names below ({@code dev}, {@code test}, {@code local}) are a reasonable guess — adjust
 * them to match whatever profile names this project actually uses for non-prod
 * environments, and double-check the prod profile is NOT in this list.</p>
 *
 * <p>Base path: {@code /api/payments/seed}</p>
 */
@RestController
@RequestMapping("/api/payments/seed")
@RequiredArgsConstructor
@Profile({"dev", "test", "local"})
public class PaymentSeedController {

    private final EntityManager entityManager;

    // -------------------------------------------------------------------------
    // POST /api/payments/seed/wallet
    // -------------------------------------------------------------------------

    /**
     * Creates a new Customer and a linked CustomerWallet with an optional starting
     * balance, then returns the IDs you need to call the real payment endpoints.
     *
     * <p>Example:
     * <pre>
     * POST /api/payments/seed/wallet?tenantId=...&currency=EGP&initialBalance=1000.00
     * </pre>
     *
     * @param currency       ISO-4217 currency code (default: "EGP")
     * @param initialBalance starting wallet balance (default: 0.00)
     * @return a JSON object with the generated customerId and walletId for copy-paste into test calls
     */
    @PostMapping("/wallet")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedWallet(
            @RequestParam(defaultValue = "EGP") String currency,
            @RequestParam(defaultValue = "0.00") BigDecimal initialBalance) {

        // Step 1: Create a minimal Customer row — email is unique so we randomise it
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        Customer customer = Customer.builder()
                .email("test-" + randomSuffix + "@seed.dev")
                .passwordHash("seeded-hash")
                .firstName("Test")
                .lastName("Customer")
                .phone("0000000000")
                .build();
        entityManager.persist(customer);

        // Flush so the customer gets its generated UUID before we reference it in the wallet
        entityManager.flush();

        // Step 2: Create the CustomerWallet linked to the new customer
        CustomerWallet wallet = CustomerWallet.builder()
                .customer(customer)
                .balance(initialBalance)
                .currency(currency)
                .build();
        entityManager.persist(wallet);

        entityManager.flush();

        // Return all IDs the tester needs to copy into subsequent API calls
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message",        "Customer and global wallet created successfully");
        response.put("customerId",     customer.getId());
        response.put("walletId",       wallet.getId());
        response.put("currency",       currency);
        response.put("initialBalance", initialBalance);
        response.put("note",           "Use customerId in your payment API calls — the wallet works across all tenants");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}