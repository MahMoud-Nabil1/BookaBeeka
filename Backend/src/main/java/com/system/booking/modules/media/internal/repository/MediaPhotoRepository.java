package com.system.booking.modules.media.internal.repository;

import com.system.booking.modules.media.internal.entity.MediaPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaPhotoRepository extends JpaRepository<MediaPhoto, UUID> {

    List<MediaPhoto> findByResourceIdOrderBySortOrderAsc(UUID resourceId);

    List<MediaPhoto> findByTenantIdAndResourceIdOrderBySortOrderAsc(UUID tenantId, UUID resourceId);

    Optional<MediaPhoto> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByResourceId(UUID resourceId);

    Optional<MediaPhoto> findByResourceIdAndIsPrimaryTrue(UUID resourceId);

    boolean existsByCloudinaryPublicId(String cloudinaryPublicId);

    List<MediaPhoto> findByTenantId(UUID tenantId);

    void deleteByTenantId(UUID tenantId);
}
