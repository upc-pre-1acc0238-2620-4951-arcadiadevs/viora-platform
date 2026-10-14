package com.arcadiadevs.viora.platform.settlement.application.commandservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SealAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/** Seals the agronomic dossier of a settled campaign for the producer (US30). */
public interface SealAgronomicDossierCommandService {

    /**
     * Seals the dossier, or returns the one already sealed for the campaign.
     *
     * @param command plot and campaign
     * @return the dossier and whether it was sealed by this call, or the failure
     */
    Result<Sealing, ApplicationError> handle(SealAgronomicDossierCommand command);

    /**
     * Outcome of a seal request.
     *
     * @param dossier the sealed dossier
     * @param created true when this call sealed it, false when it already existed
     */
    record Sealing(SealedDossierSnapshot dossier, boolean created) {
    }
}
