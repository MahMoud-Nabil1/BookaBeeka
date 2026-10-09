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
    public static final UUID ANONYMOUS_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

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

            // Ensure Platform Anonymous/Deleted Customer exists for preserving historical bookings & reviews
            String anonCustSql = """
                INSERT INTO customer (id, email, password_hash, first_name, last_name, is_active, banned, created_at, updated_at)
                VALUES ('00000000-0000-0000-0000-000000000002', 'deleted-user@platform.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi50jvd77Gq5gD0eP/84.XfQW809nku', 'Deleted', 'Customer', FALSE, TRUE, NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
            """;
            jdbcTemplate.execute(anonCustSql);

            // Also ensure customer_id on notification table is nullable for non-customer platform notifications
            try {
                jdbcTemplate.execute("ALTER TABLE notification ALTER COLUMN customer_id DROP NOT NULL;");
            } catch (Exception ex) {
                // Table might not exist yet or column already nullable; safely ignore
                log.debug("customer_id column alteration skipped or already nullable: {}", ex.getMessage());
            }

            // Ensure customer ban and tenant suspension columns exist
            try {
                jdbcTemplate.execute("ALTER TABLE customer ADD COLUMN IF NOT EXISTS banned BOOLEAN NOT NULL DEFAULT FALSE;");
                jdbcTemplate.execute("ALTER TABLE customer ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP WITHOUT TIME ZONE;");
                jdbcTemplate.execute("ALTER TABLE customer ADD COLUMN IF NOT EXISTS banned_by VARCHAR(255);");
                jdbcTemplate.execute("ALTER TABLE customer ALTER COLUMN banned_by TYPE VARCHAR(255);");
                jdbcTemplate.execute("UPDATE customer SET banned = FALSE WHERE banned IS NULL;");
                jdbcTemplate.execute("ALTER TABLE customer ADD COLUMN IF NOT EXISTS ban_reason TEXT;");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_customer_banned ON customer(banned);");

                jdbcTemplate.execute("ALTER TABLE tenant ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP WITHOUT TIME ZONE;");
                jdbcTemplate.execute("ALTER TABLE tenant ADD COLUMN IF NOT EXISTS suspended_by VARCHAR(255);");
                jdbcTemplate.execute("ALTER TABLE tenant ALTER COLUMN suspended_by TYPE VARCHAR(255);");
                jdbcTemplate.execute("ALTER TABLE tenant ADD COLUMN IF NOT EXISTS suspended_reason TEXT;");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant(status);");

                // Clean up obsolete legacy tables from deprecated branch-level architecture
                jdbcTemplate.execute("DROP TABLE IF EXISTS staff CASCADE;");
                jdbcTemplate.execute("DROP TABLE IF EXISTS time_off CASCADE;");
                jdbcTemplate.execute("DROP TABLE IF EXISTS working_hours CASCADE;");
                jdbcTemplate.execute("DROP TABLE IF EXISTS branch CASCADE;");
                log.info("SystemTenantInitializer: Dropped legacy deprecated tables if they existed.");
            } catch (Exception ex) {
                log.warn("Super admin controls DDL initialization skipped: {}", ex.getMessage());
            }

        } catch (Exception ex) {
            log.error("SystemTenantInitializer failed to initialize system tenant: {}", ex.getMessage(), ex);
        }
    }
}
