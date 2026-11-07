package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReasonType;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorAlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity mapping the {@code sector_alerts} table: the alerts of the sectors of each license, open or normalized.
 */
@Entity
@Table(name = "sector_alerts",
        indexes = @Index(name = "ix_sector_alerts_license_normalized", columnList = "license_id, normalized_at"))
@Getter
@Setter
@NoArgsConstructor
public class SectorAlertPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "license_id", nullable = false)
    private UUID licenseId;

    @Column(name = "sector_zone_id", nullable = false)
    private UUID sectorZoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private SectorAlertType type;

    @Column(name = "affected_plots", nullable = false)
    private int affectedPlots;

    @Column(name = "sector_plots", nullable = false)
    private int sectorPlots;

    @Column(name = "lead_plot_id")
    private UUID leadPlotId;

    @Column(name = "lead_producer_id")
    private UUID leadProducerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_reason", length = 30)
    private RiskReasonType leadReason;

    @Column(name = "lead_value")
    private Double leadValue;

    @Column(name = "lead_date")
    private LocalDate leadDate;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "facts_updated_at", nullable = false)
    private Instant factsUpdatedAt;

    @Column(name = "normalized_at")
    private Instant normalizedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_at_normalize", length = 20)
    private RiskLevel levelAtNormalize;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
