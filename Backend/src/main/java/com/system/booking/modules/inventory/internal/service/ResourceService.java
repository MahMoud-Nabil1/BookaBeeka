package com.system.booking.modules.inventory.internal.service;

import com.system.booking.modules.inventory.internal.dto.CreateResourceRequest;
import com.system.booking.modules.inventory.internal.dto.ResourceResponse;
import com.system.booking.modules.inventory.internal.dto.UpdateResourceRequest;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.inventory.internal.exception.ResourceNotFoundException;
import com.system.booking.modules.inventory.internal.repository.ResourceRepository;
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
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest req) {
        Branch branch = branchRepository.findById(req.branchId())
            .filter(b -> b.getTenantId().equals(req.tenantId()))
            .orElseThrow(() -> new IllegalArgumentException("Branch not found or does not belong to tenant"));

        Resource resource = Resource.builder()
            .tenantId(req.tenantId())
            .branch(branch)
            .name(req.name())
            .resourceType(req.resourceType())
            .capacity(req.capacity())
            .specs(req.specs())
            .isActive(true)
            .isBookable(true)
            .build();
            
        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse updateResource(UUID tenantId, UUID resourceId, UpdateResourceRequest req) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        if (req.name() != null) resource.setName(req.name());
        if (req.resourceType() != null) resource.setResourceType(req.resourceType());
        if (req.capacity() != null) resource.setCapacity(req.capacity());
        if (req.specs() != null) resource.setSpecs(req.specs());
        if (req.isActive() != null) resource.setIsActive(req.isActive());
        if (req.isBookable() != null) resource.setIsBookable(req.isBookable());

        resource = resourceRepository.save(resource);
        return toResponse(resource);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> listResources(UUID tenantId) {
        return resourceRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        return toResponse(resource);
    }

    @Transactional
    public void deleteResource(UUID tenantId, UUID resourceId) {
        Resource resource = resourceRepository.findByTenantIdAndId(tenantId, resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(Resource r) {
        return new ResourceResponse(
            r.getId(),
            r.getTenantId(),
            r.getBranch() != null ? r.getBranch().getId() : null,
            r.getName(),
            r.getResourceType(),
            r.getCapacity(),
            r.getSpecs(),
            r.getIsActive(),
            r.getIsBookable(),
            r.getCreatedAt()
        );
    }
}
