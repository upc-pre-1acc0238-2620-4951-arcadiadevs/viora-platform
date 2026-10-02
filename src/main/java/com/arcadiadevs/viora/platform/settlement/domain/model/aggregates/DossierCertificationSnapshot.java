package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/**
 * Immutable metadata of a dossier certification, with the identity of its report, plot and campaign.
 *
 * <p>The PDF bytes are deliberately not part of it: they live behind
 * {@link com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository}, so loading
 * a report never loads any document.</p>
 *
 * @param id           certification identifier
 * @param reportId     parent agronomic report
 * @param plotId       certified plot
 * @param campaignYear certified campaign
 * @param metadata     verification hash, signature and certification instant
 * @param certifier    declared certifying professional
 * @param notes        optional certification notes
 */
public record DossierCertificationSnapshot(
        CertificationId id,
        ReportId reportId,
        PlotId plotId,
        CampaignYear campaignYear,
        DossierMetadata metadata,
        CertifierIdentity certifier,
        String notes
) {
}
