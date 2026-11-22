package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistence of the caliber ranges of a license: one row per license, its ranges in order in their own table.
 */
@Entity
@Table(name = "caliber_range_sets", uniqueConstraints = @UniqueConstraint(name = "uk_caliber_range_sets_license",
        columnNames = "license_id"))
@Getter
@Setter
@NoArgsConstructor
public class CaliberRangeSetPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "caliber_ranges", joinColumns = @JoinColumn(name = "caliber_range_set_id"))
    @OrderColumn(name = "position")
    private List<CaliberRangeEmbeddable> ranges = new ArrayList<>();

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
