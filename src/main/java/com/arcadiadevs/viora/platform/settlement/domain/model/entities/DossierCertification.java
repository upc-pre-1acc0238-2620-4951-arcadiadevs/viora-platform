package com.arcadiadevs.viora.platform.settlement.domain.model.entities;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/** Immutable certification of one settled campaign: the hash of its stored PDF and the declared signature. */
public final class DossierCertification {

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
     * @param notes        validated optional notes
     * @return the new certification
     */
    public static DossierCertification create(ReportId reportId, PlotId plotId, CampaignYear campaignYear,
            DossierMetadata metadata, CertifierIdentity certifier, CertificationNotes notes) {
        if (reportId == null || plotId == null || campaignYear == null || metadata == null || certifier == null
                || notes == null) {
            throw new IllegalArgumentException("settlement.certification.reference.null");
        }
        return new DossierCertification(new DossierCertificationSnapshot(new CertificationId(), reportId, plotId,
                campaignYear, metadata, certifier, notes.value()));
    }

    public DossierCertificationSnapshot snapshot() {
        return state;
    }
}
