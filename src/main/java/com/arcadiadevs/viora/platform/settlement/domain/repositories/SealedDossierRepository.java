package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.SealedDossierId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;

import java.util.List;
import java.util.Optional;

/**
 * Repository port of the dossiers sealed by producers. Insert only: a sealed dossier and its bytes are never
 * updated or deleted.
 */
public interface SealedDossierRepository {

    /**
     * Stores a new dossier and its exact PDF in the same transaction.
     *
     * @param dossier  dossier metadata and facts
     * @param document exact PDF bytes
     */
    void save(SealedDossierSnapshot dossier, DossierDocument document);

    Optional<SealedDossierSnapshot> findByPlotIdAndCampaignYear(PlotId plotId, CampaignYear campaignYear);

    /**
     * Lists the dossiers of a plot, most recent campaign first.
     *
     * @param plotId the plot
     * @return its dossiers
     */
    List<SealedDossierSnapshot> findByPlotId(PlotId plotId);

    Optional<SealedDossierSnapshot> findByVerificationHash(VerificationHash verificationHash);

    Optional<DossierDocument> findDocument(SealedDossierId dossierId);
}
