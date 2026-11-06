package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code plot_sector_assignments} table: the sector each member gave each plot within the
 * license that sponsors them. One row per plot and license.
 */
@Entity
@Table(name = "plot_sector_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_plot_sector_assignments_license_plot",
                columnNames = {"license_id", "plot_id"}),
        indexes = @Index(name = "ix_plot_sector_assignments_producer", columnList = "producer_id"))
@Getter
@Setter
@NoArgsConstructor
public class PlotSectorAssignmentPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "producer_id", nullable = false)
    private UUID producerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlotSectorStatus status;

    @Column(name = "sector_zone_id")
    private UUID sectorZoneId;

    @Column(name = "proposed_name", length = 60)
    private String proposedName;

    @Column(name = "normalized_proposed_name", length = 60)
    private String normalizedProposedName;

    @Column(name = "sector_updated_at", nullable = false)
    private Instant sectorUpdatedAt;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
