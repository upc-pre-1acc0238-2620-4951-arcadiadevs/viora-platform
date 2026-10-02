package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Settlement projection of confirmed thinning executions: one per plot and campaign, one per event. */
@Entity
@Table(name = "settlement_thinning_executions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_settlement_thinning_plot_campaign", columnNames = {"plot_id", "campaign_year"}),
                @UniqueConstraint(name = "uq_settlement_thinning_event", columnNames = "event_id")
        })
@Getter
@Setter
public class ThinningExecutionRecordPersistenceEntity {
    @Id
    @Column(name = "confirmation_id", nullable = false, updatable = false)
    private UUID confirmationId;

    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(name = "plot_id", nullable = false, updatable = false)
    private UUID plotId;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;

    @Column(nullable = false, updatable = false)
    private LocalDate executedDate;
    @Column(updatable = false)
    private Double prescribedRemovalPercentage;
    @Column(nullable = false, updatable = false)
    private Double actualRemovalPercentage;
    @Column(nullable = false, updatable = false)
    private Boolean onTime;
}
