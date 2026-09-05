package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.request.CreateServiceOfferingRequest;
import com.system.booking.modules.inventory.internal.dto.request.UpdateServiceOfferingRequest;
import com.system.booking.modules.inventory.internal.dto.response.ServiceOfferingResponse;
import com.system.booking.modules.inventory.internal.entity.ServiceOffering;
import com.system.booking.modules.inventory.internal.exception.DuplicateInventoryEntityException;
import com.system.booking.modules.inventory.internal.exception.ServiceOfferingNotFoundException;
import com.system.booking.modules.inventory.internal.repository.ServiceOfferingRepository;
import com.system.booking.modules.tenant.internal.entity.Branch;
import com.system.booking.modules.tenant.internal.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public ServiceOfferingResponse createServiceOffering(UUID tenantId, CreateServiceOfferingRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to tenant"));

        if (serviceOfferingRepository.existsByTenantIdAndBranchIdAndNameIgnoreCase(tenantId, req.branchId(), req.name().trim())) {
            throw new DuplicateInventoryEntityException("Service offering '" + req.name() + "' already exists in this branch");
        }

        ServiceOffering offering = ServiceOffering.builder()
                .tenantId(tenantId)
                .branch(branch)
                .name(req.name().trim())
                .description(req.description())
                .price(req.price())
                .durationMinutes(req.durationMinutes())
                .bufferMinutes(req.bufferMinutes())
                .customAttributes(req.customAttributes())
                .isActive(true)
                .build();

        offering = serviceOfferingRepository.save(offering);
        return toResponse(offering);
    }

    @Transactional
    public ServiceOfferingResponse updateServiceOffering(UUID tenantId, UUID serviceOfferingId, UpdateServiceOfferingRequest req) {
        ServiceOffering offering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(serviceOfferingId));

        if (req.name() != null && !req.name().trim().equalsIgnoreCase(offering.getName())) {
            if (serviceOfferingRepository.existsByTenantIdAndBranchIdAndNameIgnoreCaseAndIdNot(
                    tenantId, offering.getBranch().getId(), req.name().trim(), serviceOfferingId)) {
                throw new DuplicateInventoryEntityException("Service offering '" + req.name() + "' already exists in this branch");
            }
            offering.setName(req.name().trim());
        }

        if (req.description() != null) offering.setDescription(req.description());
        if (req.price() != null) offering.setPrice(req.price());
        if (req.durationMinutes() != null) offering.setDurationMinutes(req.durationMinutes());
        if (req.bufferMinutes() != null) offering.setBufferMinutes(req.bufferMinutes());
        if (req.customAttributes() != null) offering.setCustomAttributes(req.customAttributes());
        if (req.isActive() != null) offering.setIsActive(req.isActive());

        offering = serviceOfferingRepository.save(offering);
        return toResponse(offering);
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listServiceOfferings(UUID tenantId) {
        return serviceOfferingRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponse> listServiceOfferingsByBranch(UUID tenantId, UUID branchId) {
        return serviceOfferingRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceOfferingResponse getServiceOffering(UUID tenantId, UUID serviceOfferingId) {
        ServiceOffering offering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(serviceOfferingId));
        return toResponse(offering);
    }

    @Transactional
    public void deleteServiceOffering(UUID tenantId, UUID serviceOfferingId) {
        ServiceOffering offering = serviceOfferingRepository.findByTenantIdAndId(tenantId, serviceOfferingId)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(serviceOfferingId));
        serviceOfferingRepository.delete(offering);
    }

    private ServiceOfferingResponse toResponse(ServiceOffering o) {
        return new ServiceOfferingResponse(
                o.getId(),
                o.getTenantId(),
                o.getBranch() != null ? o.getBranch().getId() : null,
                o.getName(),
                o.getDescription(),
                o.getPrice(),
                o.getDurationMinutes(),
                o.getBufferMinutes(),
                o.getCustomAttributes(),
                o.getIsActive(),
                o.getCreatedAt() != null ? o.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}