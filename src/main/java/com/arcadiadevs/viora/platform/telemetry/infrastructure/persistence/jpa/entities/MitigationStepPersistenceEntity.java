package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a persisted mitigation step child record.
 */
@Entity
@Table(name = "agroclimatic_incident_mitigation_steps")
@Getter
@Setter
@NoArgsConstructor
public class MitigationStepPersistenceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private AgroclimaticIncidentPersistenceEntity incident;

    @Column(name = "instruction_key", nullable = false, length = 150)
    private String instructionKey;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;
}
