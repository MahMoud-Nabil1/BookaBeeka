package com.system.booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ensures the platform system tenant (00000000-0000-0000-0000-000000000000) exists
 * in the tenant table so that platform-level notifications (such as OTP requests and
 * password resets for global customers) do not violate foreign key constraints.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class SystemTenantInitializer implements ApplicationRunner {

    public static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("SystemTenantInitializer: Ensuring Platform System Tenant exists in database...");
            String sql = """
                INSERT INTO tenant (id, name, subdomain, status, timezone, currency, created_at, updated_at)
                VALUES ('00000000-0000-0000-0000-000000000000', 'Platform System', 'system-platform', 'ACTIVE', 'UTC', 'USD', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
            """;
            jdbcTemplate.execute(sql);
            log.info("SystemTenantInitializer: Platform System Tenant ({}) is ready.", SYSTEM_TENANT_ID);

            // Also ensure customer_id on notification table is nullable for non-customer platform notifications
            try {
                jdbcTemplate.execute("ALTER TABLE notification ALTER COLUMN customer_id DROP NOT NULL;");
            } catch (Exception ex) {
                // Table might not exist yet or column already nullable; safely ignore
                log.debug("customer_id column alteration skipped or already nullable: {}", ex.getMessage());
            }

        } catch (Exception ex) {
            log.error("SystemTenantInitializer failed to initialize system tenant: {}", ex.getMessage(), ex);
        }
    }
}
