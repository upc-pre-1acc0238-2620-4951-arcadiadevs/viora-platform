package com.arcadiadevs.viora.platform.settlement.application.queryservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierByVerificationHashQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierPdfByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossiersByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;

/** Reads the dossiers sealed by producers. */
public interface SealedDossierQueryService {

    /**
     * Reads the dossier of a campaign with the agronomist certification of that campaign, if any.
     *
     * @param query the plot, the campaign and the producer asking
     * @return the dossier, or not found / forbidden errors
     */
    Result<DossierView, ApplicationError> handle(GetSealedDossierByPlotIdAndCampaignYearQuery query);

    /**
     * Lists the dossiers of a plot, most recent campaign first.
     *
     * @param query the plot and the producer asking
     * @return the dossiers, empty when none was sealed
     */
    Result<List<SealedDossierSnapshot>, ApplicationError> handle(GetSealedDossiersByPlotIdQuery query);

    /**
     * Reads the exact PDF of a campaign.
     *
     * @param query the plot, the campaign and the producer asking
     * @return the dossier and its bytes
     */
    Result<DossierFile, ApplicationError> handle(GetSealedDossierPdfByPlotIdAndCampaignYearQuery query);

    /**
     * Finds the dossier whose stored PDF has a SHA-256. It does not require the plot to be active: a document handed
     * to a bank or a cooperative must stay verifiable.
     *
     * @param query the SHA-256
     * @return the matching dossier, or not found
     */
    Result<SealedDossierSnapshot, ApplicationError> handle(GetSealedDossierByVerificationHashQuery query);

    /**
     * A dossier and the agronomist certification of the same campaign, if one exists.
     *
     * @param dossier       the sealed dossier
     * @param certification the certification of ADR-002, or null
     */
    record DossierView(SealedDossierSnapshot dossier, DossierCertificationSnapshot certification) {
    }

    /**
     * A dossier and its exact bytes.
     *
     * @param dossier  the sealed dossier
     * @param document the PDF
     */
    record DossierFile(SealedDossierSnapshot dossier, DossierDocument document) {
    }
}
