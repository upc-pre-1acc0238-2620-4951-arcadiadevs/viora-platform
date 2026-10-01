package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing the persistence table {@code crop_load.field_sampling_rounds}.
 */
@Entity
@Table(
        name = "field_sampling_rounds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_sampling_idempotency",
                        columnNames = {"actor_id", "plot_id", "client_batch_id"}
                )
        }
)
public class SamplingRoundPersistenceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private FruitThinningPrescriptionPersistenceEntity prescription;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "client_batch_id", nullable = false, length = 64)
    private String clientBatchId;

    @Column(name = "is_representative", nullable = false)
    private Boolean isRepresentative;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TreeSamplingRecordPersistenceEntity> samplingRecords = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SamplingRoundPersistenceEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FruitThinningPrescriptionPersistenceEntity getPrescription() {
        return prescription;
    }

    public void setPrescription(FruitThinningPrescriptionPersistenceEntity prescription) {
        this.prescription = prescription;
    }

    public UUID getPlotId() {
        return plotId;
    }

    public void setPlotId(UUID plotId) {
        this.plotId = plotId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getClientBatchId() {
        return clientBatchId;
    }

    public void setClientBatchId(String clientBatchId) {
        this.clientBatchId = clientBatchId;
    }

    public Boolean getIsRepresentative() {
        return isRepresentative;
    }

    public void setIsRepresentative(Boolean isRepresentative) {
        this.isRepresentative = isRepresentative;
    }

    public List<TreeSamplingRecordPersistenceEntity> getSamplingRecords() {
        return samplingRecords;
    }

    public void setSamplingRecords(List<TreeSamplingRecordPersistenceEntity> samplingRecords) {
        this.samplingRecords = samplingRecords;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
