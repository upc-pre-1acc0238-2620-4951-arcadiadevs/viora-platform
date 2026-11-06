package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity mapping the {@code sector_zones} table: the sector list of each cooperative license. The normalized
 * name is unique per license, so two zones that differ only in accents or case cannot exist.
 */
@Entity
@Table(name = "sector_zones", uniqueConstraints = @UniqueConstraint(name = "uk_sector_zones_license_name",
        columnNames = {"license_id", "normalized_name"}))
@Getter
@Setter
@NoArgsConstructor
public class SectorZonePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 60)
    private String normalizedName;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
