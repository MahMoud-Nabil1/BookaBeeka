package com.system.booking.modules.owner.internal.service;

import com.system.booking.modules.hoteladmin.port.in.HotelAdminProvisioningPort;
import com.system.booking.modules.owner.internal.dto.*;
import com.system.booking.modules.owner.internal.entity.Owner;
import com.system.booking.modules.owner.internal.repository.OwnerReportingRepository;
import com.system.booking.modules.owner.internal.repository.OwnerRepository;
import com.system.booking.modules.payment.internal.repository.TenantWalletRepository;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import com.system.booking.modules.tenant.internal.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerReportingRepository reportingRepository;
    private final HotelAdminProvisioningPort hotelAdminPort;
    private final TenantRepository tenantRepository;
    private final TenantWalletRepository tenantWalletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppointAdminResponse registerOwner(OwnerRegisterRequest request) {
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw new IllegalArgumentException("Subdomain '" + request.subdomain() + "' is already taken");
        }
        if (ownerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered");
        }

        Tenant tenant = Tenant.builder()
                .name(request.hotelName())
                .subdomain(request.subdomain())
                .status("ACTIVE")
                .currency(request.currency() != null ? request.currency() : "USD")
                .timezone(request.timezone() != null ? request.timezone() : "UTC")
                .build();
        tenant = tenantRepository.save(tenant);

        Owner owner = Owner.builder()
                .tenantId(tenant.getId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .isActive(true)
                .build();
        owner = ownerRepository.save(owner);

        return new AppointAdminResponse(
                owner.getId(),
                owner.getFirstName() + " " + owner.getLastName(),
                owner.getEmail(),
                "OWNER",
                "Hotel and Owner account created successfully"
        );
    }

    @Transactional
    public AppointAdminResponse appointAdmin(UUID tenantId, AppointAdminRequest request) {
        UUID newAdminId = hotelAdminPort.createAdmin(
                tenantId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.phone()
        );

        return new AppointAdminResponse(
                newAdminId,
                request.firstName() + " " + request.lastName(),
                request.email(),
                "ADMIN",
                "Admin appointed successfully to your hotel."
        );
    }

    @Transactional(readOnly = true)
    public List<OwnerAdminSummaryDto> listAdmins(UUID tenantId) {
        return reportingRepository.listTenantAdmins(tenantId);
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        BigDecimal overallRevenue = reportingRepository.getOverallTenantRevenue(tenantId);
        long totalBookings = reportingRepository.getTotalBookingCount(tenantId);
        long totalAdmins = hotelAdminPort.countAdminsByTenantId(tenantId);

        BigDecimal walletBalance = tenantWalletRepository.findByTenantId(tenantId)
                .map(w -> w.getBalance())
                .orElse(BigDecimal.ZERO);

        OwnerTenantSummaryDto tenantSummary = new OwnerTenantSummaryDto(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getStatus(),
                tenant.getTimezone(),
                tenant.getCurrency(),
                tenant.getCreatedAt(),
                0L,
                overallRevenue
        );

        return new OwnerDashboardResponse(
                tenantSummary,
                overallRevenue,
                walletBalance,
                tenant.getCurrency() != null ? tenant.getCurrency() : "USD",
                totalBookings,
                totalAdmins
        );
    }

    @Transactional(readOnly = true)
    public OwnerRevenueSummaryDto getRevenueSummary(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        BigDecimal overallRevenue = reportingRepository.getOverallTenantRevenue(tenantId);
        long completedBookings = reportingRepository.getCompletedBookingCount(tenantId);

        BigDecimal walletBalance = tenantWalletRepository.findByTenantId(tenantId)
                .map(w -> w.getBalance())
                .orElse(BigDecimal.ZERO);

        return new OwnerRevenueSummaryDto(
                tenant.getId(),
                tenant.getName(),
                overallRevenue,
                walletBalance,
                tenant.getCurrency() != null ? tenant.getCurrency() : "USD",
                completedBookings
        );
    }
}