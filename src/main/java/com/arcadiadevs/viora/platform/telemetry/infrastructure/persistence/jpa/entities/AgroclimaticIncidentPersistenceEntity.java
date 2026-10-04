package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.converters.PlotIdPersistenceConverter;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.embeddables.ThresholdBreachInfoPersistenceEmbeddable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity mapping the {@code agroclimatic_incidents} database table.
 */
@Entity
@Table(name = "agroclimatic_incidents")
@Getter
@Setter
@NoArgsConstructor
public class AgroclimaticIncidentPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Convert(converter = PlotIdPersistenceConverter.class)
    @Column(name = "plot_id", nullable = false)
    private PlotId plotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IncidentStatus status;

    @Embedded
    private ThresholdBreachInfoPersistenceEmbeddable breachInfo;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "stress_duration_minutes", nullable = false)
    private Long stressDurationMinutes = 0L;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MitigationStepPersistenceEntity> mitigationSteps = new ArrayList<>();

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
