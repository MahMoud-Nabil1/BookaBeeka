package com.system.booking.modules.staff.internal.dto.response;

import com.system.booking.modules.staff.internal.entity.Staff;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a staff member's public-facing data.
 *
 * <p>This record maps a {@link Staff} entity to a safe API output,
 * deliberately excluding sensitive fields like {@code passwordHash}
 * and redundant fields like {@code tenantId} (the caller already
 * knows their own tenant).</p>
 *
 * <p><b>Usage:</b> Returned by all Staff CRUD endpoints via
 * {@code StaffController}.</p>
 */
public record StaffResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role,
        Boolean isActive,
        UUID branchId,
        LocalDateTime createdAt
) {

    /**
     * Factory method to convert a {@link Staff} entity into a {@code StaffResponse}.
     *
     * <p>Extracts the branch ID from the lazy-loaded {@code Branch} relationship.
     * This is safe because the entity is still within a transactional context
     * when this method is called from the service layer.</p>
     *
     * @param staff the JPA entity to convert
     * @return a new StaffResponse populated from the entity
     */
    public static StaffResponse fromEntity(Staff staff) {
        return new StaffResponse(
                staff.getId(),
                staff.getEmail(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getPhone(),
                staff.getRole(),
                staff.getIsActive(),
                staff.getBranch().getId(),
                staff.getCreatedAt()
        );
    }
}
