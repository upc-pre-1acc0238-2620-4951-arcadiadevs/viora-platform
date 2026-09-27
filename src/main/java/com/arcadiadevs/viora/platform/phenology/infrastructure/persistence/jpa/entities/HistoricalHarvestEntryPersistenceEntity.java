package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code phenology.harvest_records} relational database table.
 */
@Entity
@Table(name = "harvest_records", schema = "phenology")
@Getter
@Setter
@NoArgsConstructor
public class HistoricalHarvestEntryPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id", nullable = false)
    private ChillAccumulationTrackerPersistenceEntity tracker;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "campaign_year", nullable = false)
    private Integer campaignYear;

    @Column(name = "total_yield_kg", nullable = false)
    private Double totalYieldKg;

    @Column(name = "green_kg", nullable = false)
    private Double greenKg;

    @Column(name = "black_kg", nullable = false)
    private Double blackKg;

    @Column(name = "bearing_classification", nullable = false, length = 30)
    private String bearingClassification;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
