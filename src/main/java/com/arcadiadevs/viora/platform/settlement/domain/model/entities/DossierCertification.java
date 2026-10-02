package com.arcadiadevs.viora.platform.settlement.domain.model.entities;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/** Immutable certification of one settled campaign: the stored PDF, its hash and the declared signature. */
public final class DossierCertification {

    public static final int MAX_NOTES_LENGTH = 1000;

    private final DossierCertificationSnapshot state;

    private DossierCertification(DossierCertificationSnapshot state) {
        this.state = state;
    }

    /**
     * Creates the certification of a campaign.
     *
     * @param reportId     report of the plot
     * @param plotId       certified plot
     * @param campaignYear certified campaign
     * @param metadata     hash of the stored bytes, signature and instant
     * @param certifier    declared certifying professional
     * @param notes        optional notes, at most {@value #MAX_NOTES_LENGTH} characters
     * @param document     exact PDF bytes whose digest is in the metadata
     * @return the new certification
     */
    public static DossierCertification create(ReportId reportId, PlotId plotId, CampaignYear campaignYear,
            DossierMetadata metadata, CertifierIdentity certifier, String notes, DossierDocument document) {
        if (reportId == null || plotId == null || campaignYear == null || metadata == null || certifier == null
                || document == null) {
            throw new IllegalArgumentException("settlement.certification.reference.null");
        }
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("settlement.certification.notes.too_long");
        }
        return new DossierCertification(new DossierCertificationSnapshot(new CertificationId(), reportId, plotId,
                campaignYear, metadata, certifier, notes, document));
    }

    public DossierCertificationSnapshot snapshot() {
        return state;
    }
}
