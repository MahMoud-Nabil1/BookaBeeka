package com.system.booking.modules.inventory.internal.repository;

import com.system.booking.modules.inventory.internal.entity.ResourceServiceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceServiceLinkRepository extends JpaRepository<ResourceServiceLink, UUID> {
    List<ResourceServiceLink> findByResourceId(UUID resourceId);
    List<ResourceServiceLink> findByServiceOfferingId(UUID serviceOfferingId);
    Optional<ResourceServiceLink> findByResourceIdAndServiceOfferingId(UUID resourceId, UUID serviceOfferingId);
    void deleteByResourceIdAndServiceOfferingId(UUID resourceId, UUID serviceOfferingId);
}
