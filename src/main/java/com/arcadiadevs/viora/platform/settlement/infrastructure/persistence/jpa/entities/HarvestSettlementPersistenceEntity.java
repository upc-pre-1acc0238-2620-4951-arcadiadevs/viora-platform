package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Immutable settlement voucher of one campaign: weights, thinning balance and stabilization curve frozen at
 * settlement time. Only the status may change later (certification).
 */
@Entity
@Table(name = "harvest_settlements",
        uniqueConstraints = @UniqueConstraint(name = "uq_settlement_report_campaign",
                columnNames = {"report_id", "campaign_year"}))
@Getter
@Setter
public class HarvestSettlementPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private AgronomicReportPersistenceEntity report;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;
    @Column(nullable = false, updatable = false)
    private Double greenOlivesKg;
    @Column(nullable = false, updatable = false)
    private Double blackOlivesKg;
    @Column(nullable = false, updatable = false)
    private Double totalYieldKg;
    @Column(updatable = false)
    private Double commercialFruitsPerKg;
    @Column(updatable = false, length = 1000)
    private String notes;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, updatable = false)
    private Instant settledAt;

    // Thinning balance frozen at settlement
    @Column(nullable = false, updatable = false, length = 20)
    private String thinningStatus;
    @Column(updatable = false)
    private LocalDate thinningExecutedDate;
    @Column(updatable = false)
    private Double prescribedRemovalPercentage;
    @Column(updatable = false)
    private Double actualRemovalPercentage;
    @Column(updatable = false)
    private Double removalDeviationPoints;

    // Stabilization curve frozen at settlement
    @Column(nullable = false, updatable = false, length = 30)
    private String curveStatus;
    @Column(nullable = false, updatable = false)
    private Integer baselineCampaigns;
    @Column(nullable = false, updatable = false)
    private Integer settledCampaigns;
    @Column(updatable = false)
    private Double baselineYieldKg;
    @Column(updatable = false)
    private Double baselineAlternationIndex;
    @Column(updatable = false)
    private Double managedAlternationIndex;
    @Column(updatable = false)
    private Double amplitudeReductionRate;
    @Column(updatable = false)
    private Boolean stabilizationTargetAchieved;
    @Column(updatable = false)
    private Double interannualVarianceKg2;
    @Column(updatable = false)
    private Double coefficientOfVariation;
}
