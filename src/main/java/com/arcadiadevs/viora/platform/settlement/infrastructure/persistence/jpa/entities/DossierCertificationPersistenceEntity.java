package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable certification of one settled campaign: the SHA-256 of its PDF and the declared signature. The PDF
 * bytes live in {@link DossierDocumentPersistenceEntity}. Every column is non-updatable; the unique {@code (report_id, campaign_year)} pair turns a certification race
 * into a conflict.
 */
@Entity
@Table(name = "dossier_certifications",
        uniqueConstraints = @UniqueConstraint(name = "uq_certification_report_campaign",
                columnNames = {"report_id", "campaign_year"}))
@Getter
@Setter
public class DossierCertificationPersistenceEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private AgronomicReportPersistenceEntity report;

    @Column(name = "campaign_year", nullable = false, updatable = false)
    private Integer campaignYear;

    @Column(name = "verification_hash", nullable = false, updatable = false, length = 64)
    private String verificationHash;

    @Column(name = "auditor_signature", nullable = false, updatable = false, length = 120)
    private String auditorSignature;

    @Column(name = "certified_by", nullable = false, updatable = false, length = 120)
    private String certifiedBy;

    @Column(name = "cip_number", nullable = false, updatable = false, length = 20)
    private String cipNumber;

    @Column(updatable = false, length = 1000)
    private String notes;

    @Column(name = "certified_at", nullable = false, updatable = false)
    private Instant certifiedAt;
}
