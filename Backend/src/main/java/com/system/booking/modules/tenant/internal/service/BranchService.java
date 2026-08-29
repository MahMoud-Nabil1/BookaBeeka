package com.system.booking.modules.tenant.internal.service;

import com.system.booking.modules.tenant.api.dto.BranchDto;
import com.system.booking.modules.tenant.api.dto.CreateBranchRequest;
import com.system.booking.modules.tenant.api.dto.UpdateBranchRequest;
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
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchDto createBranch(UUID tenantId, CreateBranchRequest request) {
        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setStatus("ACTIVE");
        branch.setSettings(request.settings());

        Branch savedBranch = branchRepository.save(branch);
        return toDto(savedBranch);
    }

    public BranchDto getBranchById(UUID tenantId, UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        return toDto(branch);
    }

    public List<BranchDto> listBranchesByTenant(UUID tenantId) {
        return branchRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchDto updateBranch(UUID tenantId, UUID branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (request.name() != null) {
            branch.setName(request.name());
        }
        if (request.address() != null) {
            branch.setAddress(request.address());
        }
        if (request.settings() != null) {
            branch.setSettings(request.settings());
        }

        Branch savedBranch = branchRepository.save(branch);
        return toDto(savedBranch);
    }

    private BranchDto toDto(Branch branch) {
        return new BranchDto(
                branch.getId(),
                branch.getTenantId(),
                branch.getName(),
                branch.getAddress(),
                branch.getStatus(),
                branch.getSettings(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}
