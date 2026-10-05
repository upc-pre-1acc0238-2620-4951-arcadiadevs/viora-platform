package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** One real calibration point per plot and campaign: residual load after on-time thinning and harvest caliber. */
@Entity
@Table(
        name = "caliber_calibration_observations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_caliber_observation_plot_campaign",
                columnNames = {"plot_id", "campaign_year"}
        ),
        indexes = @Index(name = "ix_caliber_observation_variety", columnList = "variety")
)
@Getter
@Setter
public class CaliberCalibrationObservationPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "campaign_year", nullable = false)
    private Integer campaignYear;

    @Column(nullable = false, length = 20)
    private String variety;

    // The column keeps its old name on purpose: it is NOT NULL, and a renamed column would leave the old one
    // behind making every insert fail on an existing database. It holds fruits per shoot (it held per-0.20 m values).
    @Column(name = "residual_fruits_per_meter", nullable = false)
    private Double residualFruitsPerShoot;

    @Column(nullable = false)
    private Double commercialFruitsPerKg;
}
