package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Immutable execution evidence; the unique foreign key enforces one confirmation per prescription. */
@Entity
@Table(name = "execution_confirmations")
@Getter
@Setter
public class ExecutionConfirmationPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false, unique = true, updatable = false)
    private FruitThinningPrescriptionPersistenceEntity prescription;

    @Column(nullable = false, updatable = false)
    private LocalDate executionDate;
    @Column(nullable = false, updatable = false)
    private Double actualRemovalPercentage;
    @Column(nullable = false, updatable = false)
    private Double removedKg;
    @Column(nullable = false, updatable = false)
    private Integer laborCrewSize;
    @Column(nullable = false, updatable = false, length = 10)
    private String timeliness;
    @Column(nullable = false, updatable = false)
    private Instant recordedAt;
    @Column(updatable = false, length = 2000)
    private String notes;

    // Load balance left by the labor (nullable only for confirmations recorded before it existed)
    @Column(updatable = false)
    private Double preThinningFruitsPerShoot;
    @Column(updatable = false)
    private Double residualFruitsPerShoot;
    @Column(updatable = false)
    private Double targetFruitsPerShoot;
    @Column(updatable = false)
    private Double deltaFruitsPerShoot;
    @Column(updatable = false)
    private Double loadRatio;
    @Column(updatable = false, length = 20)
    private String loadState;

    // Caliber projection issued with the confirmation; figures are null unless ESTIMATED
    @Column(updatable = false, length = 30)
    private String caliberStatus;
    @Column(updatable = false)
    private Double caliberMostLikelyFruitsPerKg;
    @Column(updatable = false)
    private Double caliberFruitsPerKgLow;
    @Column(updatable = false)
    private Double caliberFruitsPerKgHigh;
    @Column(updatable = false)
    private Double caliberConfidenceLevel;
    @Column(updatable = false)
    private Integer caliberCalibrationObservations;
    @Column(updatable = false, length = 30)
    private String caliberModelVersion;
}
