package com.system.booking.modules.media.internal.entity;

import com.system.booking.common.model.TenantBaseEntity;
import com.system.booking.modules.inventory.internal.entity.Resource;
import com.system.booking.modules.tenant.internal.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Stores metadata for a Cloudinary-hosted photo associated with a hotel room (Resource).
 *
 * <p>Image binaries are stored in Cloudinary — this entity holds only metadata and
 * the Cloudinary public_id needed for asset management/deletion.</p>
 *
 * <p><b>Tenant isolation:</b> Every operation validates
 * {@code currentTenant == mediaPhoto.tenantId == resource.tenantId}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "media_photo", indexes = {
        @Index(name = "idx_media_photo_resource", columnList = "resource_id"),
        @Index(name = "idx_media_photo_tenant", columnList = "tenant_id")
})
public class MediaPhoto extends TenantBaseEntity {

    // ── Ownership ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    // ── Cloudinary Metadata ──

    @Column(name = "cloudinary_public_id", nullable = false, unique = true)
    private String cloudinaryPublicId;

    @Column(name = "secure_url", nullable = false, length = 500)
    private String secureUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "format", length = 10)
    private String format;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "bytes")
    private Long bytes;

    // ── Display ──

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "alt_text", length = 255)
    private String altText;
}
