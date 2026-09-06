package com.system.booking.modules.hoteladmin.port.in;

import java.util.UUID;

public interface HotelAdminProvisioningPort {

    UUID createAdmin(
            UUID tenantId,
            String firstName,
            String lastName,
            String email,
            String plainPassword,
            String phone
    );
    long countAdminsByTenantId(UUID tenantId);
}