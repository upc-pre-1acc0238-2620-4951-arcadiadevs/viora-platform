package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/**
 * Immutable state of a dossier certification, with the identity of its report, plot and campaign.
 *
 * @param id           certification identifier
 * @param reportId     parent agronomic report
 * @param plotId       certified plot
 * @param campaignYear certified campaign
 * @param metadata     verification hash, signature and certification instant
 * @param certifier    declared certifying professional
 * @param notes        optional certification notes
 * @param document     exact stored PDF bytes (copied on every access)
 */
public record DossierCertificationSnapshot(
        CertificationId id,
        ReportId reportId,
        PlotId plotId,
        CampaignYear campaignYear,
        DossierMetadata metadata,
        CertifierIdentity certifier,
        String notes,
        DossierDocument document
) {
}
