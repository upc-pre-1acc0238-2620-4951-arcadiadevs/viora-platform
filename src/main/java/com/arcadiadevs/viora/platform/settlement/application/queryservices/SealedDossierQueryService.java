package com.arcadiadevs.viora.platform.settlement.application.queryservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;

/** Reads the dossiers sealed by producers (US30). */
public interface SealedDossierQueryService {

    /**
     * Reads the dossier of a campaign with the agronomist certification of that campaign, if any.
     *
     * @param plotId       plot UUID
     * @param campaignYear campaign
     * @param actorId      producer asking; only the owner of the plot can read its dossiers
     * @return the dossier, or not found / forbidden / validation errors
     */
    Result<DossierView, ApplicationError> findByPlotAndCampaign(String plotId, Integer campaignYear, String actorId);

    /**
     * Lists the dossiers of a plot, most recent campaign first.
     *
     * @param plotId  plot UUID
     * @param actorId producer asking
     * @return the dossiers, empty when none was sealed
     */
    Result<List<SealedDossierSnapshot>, ApplicationError> findByPlot(String plotId, String actorId);

    /**
     * Reads the exact PDF of a campaign.
     *
     * @param plotId       plot UUID
     * @param campaignYear campaign
     * @param actorId      producer asking
     * @return the dossier and its bytes
     */
    Result<DossierFile, ApplicationError> findDocument(String plotId, Integer campaignYear, String actorId);

    /**
     * Finds the dossier whose stored PDF has a SHA-256. It does not require the plot to be active: a document handed
     * to a bank or a cooperative must stay verifiable.
     *
     * @param verificationHash 64 hexadecimal characters
     * @return the matching dossier, or not found
     */
    Result<SealedDossierSnapshot, ApplicationError> verify(String verificationHash);

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
