package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadata and frozen facts of a dossier sealed by the producer. Every column is insert only; the PDF bytes live in
 * {@code sealed_dossier_documents}.
 */
@Entity
@Table(name = "sealed_dossiers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_sealed_dossier_plot_campaign", columnNames = {"plot_id", "campaign_year"}),
                @UniqueConstraint(name = "uq_sealed_dossier_hash", columnNames = {"verification_hash"})
        },
        indexes = @Index(name = "ix_sealed_dossier_plot", columnList = "plot_id"))
@Getter
@Setter
public class SealedDossierPersistenceEntity implements Persistable<UUID> {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plot_id", nullable = false, updatable = false, length = 36)
    private String plotId;

    @Column(name = "producer_id", nullable = false, updatable = false, length = 36)
    private String producerId;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;

    @Column(name = "verification_hash", nullable = false, updatable = false, length = 64)
    private String verificationHash;

    @Column(name = "sealed_at", nullable = false, updatable = false)
    private Instant sealedAt;

    @Column(name = "page_count", nullable = false, updatable = false)
    private Integer pageCount;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private Integer sizeBytes;

    @Column(name = "plot_name", updatable = false, length = 120)
    private String plotName;

    @Column(name = "holder_name", updatable = false, length = 200)
    private String holderName;

    @Column(name = "variety", updatable = false, length = 40)
    private String variety;

    @Column(name = "area_hectares", updatable = false)
    private Double areaHectares;

    @Column(name = "pre_thinning_fruits_per_shoot", updatable = false)
    private Double preThinningFruitsPerShoot;

    @Column(name = "target_fruits_per_shoot", updatable = false)
    private Double targetFruitsPerShoot;

    @Column(name = "prescribed_removal_percentage", updatable = false)
    private Double prescribedRemovalPercentage;

    @Column(name = "window_opens_on", updatable = false)
    private LocalDate windowOpensOn;

    @Column(name = "window_closes_on", updatable = false)
    private LocalDate windowClosesOn;

    @Column(name = "actual_removal_percentage", updatable = false)
    private Double actualRemovalPercentage;

    @Column(name = "thinned_on", updatable = false)
    private LocalDate thinnedOn;

    @Column(name = "thinning_compliance", nullable = false, updatable = false, length = 30)
    private String thinningCompliance;

    @Column(name = "total_kg", nullable = false, updatable = false)
    private Double totalKg;

    @Column(name = "green_kg", nullable = false, updatable = false)
    private Double greenKg;

    @Column(name = "black_kg", nullable = false, updatable = false)
    private Double blackKg;

    @Column(name = "receipt_number", updatable = false, length = 40)
    private String receiptNumber;

    @Column(name = "weighed_on", updatable = false)
    private LocalDate weighedOn;

    @Column(name = "linked_nodes", nullable = false, updatable = false)
    private Integer linkedNodes;

    @Column(name = "days_with_readings", nullable = false, updatable = false)
    private Integer daysWithReadings;

    @Column(name = "history_from_year", updatable = false)
    private Integer historyFromYear;

    @Column(name = "history_to_year", updatable = false)
    private Integer historyToYear;

    @Column(name = "harvest_campaigns", nullable = false, updatable = false)
    private Integer harvestCampaigns;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
