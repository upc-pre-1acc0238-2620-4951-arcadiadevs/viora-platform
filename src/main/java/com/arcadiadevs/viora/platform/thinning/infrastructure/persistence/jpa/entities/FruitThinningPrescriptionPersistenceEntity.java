package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing the persistence table {@code crop_load.thinning_prescriptions}.
 */
@Entity
@Table(
        name = "thinning_prescriptions",
        schema = "crop_load",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_active_prescription_per_plot_campaign",
                        columnNames = {"plot_id", "campaign_year"}
                )
        }
)
public class FruitThinningPrescriptionPersistenceEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plot_id", nullable = false)
    private UUID plotId;

    @Column(name = "campaign_year", nullable = false)
    private Integer campaignYear;

    @Column(name = "observed_plot_revision")
    private Long observedPlotRevision;

    @Column(name = "target_fruits_m")
    private Double targetFruitsPerMeter;

    @Column(name = "percentage_remove")
    private Double percentageToRemove;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "window_closes_on")
    private java.time.LocalDate windowClosesOn;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SamplingRoundPersistenceEntity> samplingRounds = new ArrayList<>();

    @Version
    @Column(name = "revision")
    private Long revision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FruitThinningPrescriptionPersistenceEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlotId() {
        return plotId;
    }

    public void setPlotId(UUID plotId) {
        this.plotId = plotId;
    }

    public Integer getCampaignYear() {
        return campaignYear;
    }

    public void setCampaignYear(Integer campaignYear) {
        this.campaignYear = campaignYear;
    }

    public Long getObservedPlotRevision() {
        return observedPlotRevision;
    }

    public void setObservedPlotRevision(Long observedPlotRevision) {
        this.observedPlotRevision = observedPlotRevision;
    }

    public Double getTargetFruitsPerMeter() {
        return targetFruitsPerMeter;
    }

    public void setTargetFruitsPerMeter(Double targetFruitsPerMeter) {
        this.targetFruitsPerMeter = targetFruitsPerMeter;
    }

    public Double getPercentageToRemove() {
        return percentageToRemove;
    }

    public void setPercentageToRemove(Double percentageToRemove) {
        this.percentageToRemove = percentageToRemove;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.LocalDate getWindowClosesOn() {
        return windowClosesOn;
    }

    public void setWindowClosesOn(java.time.LocalDate windowClosesOn) {
        this.windowClosesOn = windowClosesOn;
    }

    public List<SamplingRoundPersistenceEntity> getSamplingRounds() {
        return samplingRounds;
    }

    public void setSamplingRounds(List<SamplingRoundPersistenceEntity> samplingRounds) {
        this.samplingRounds = samplingRounds;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
