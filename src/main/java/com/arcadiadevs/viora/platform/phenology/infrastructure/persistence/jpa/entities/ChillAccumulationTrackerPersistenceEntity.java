package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping the {@code phenology.chill_trackers} relational database table.
 */
@Entity
@Table(name = "chill_trackers")
@Getter
@Setter
@NoArgsConstructor
public class ChillAccumulationTrackerPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "plot_id", nullable = false, unique = true)
    private UUID plotId;

    @Column(name = "campaign_year", nullable = false)
    private Integer campaignYear;

    @Column(name = "erez_portions", nullable = false)
    private Double erezPortions;

    @Column(name = "calculated_bbi", nullable = false)
    private Double calculatedBbi;

    @OneToMany(mappedBy = "tracker", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HistoricalHarvestEntryPersistenceEntity> harvestRecords = new ArrayList<>();

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
