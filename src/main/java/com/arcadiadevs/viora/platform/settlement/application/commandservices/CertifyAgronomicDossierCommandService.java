package com.arcadiadevs.viora.platform.settlement.application.commandservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.CertifyAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/** Use case: certify the dossier of one settled campaign of a plot. */
public interface CertifyAgronomicDossierCommandService {

    /**
     * Certifies a campaign dossier: renders its PDF, stores the exact bytes with their SHA-256 and publishes the
     * generated event.
     *
     * @param command validated certification instruction
     * @return the stored certification, or the application error (validation, plot not found, campaign not settled,
     *         conflict, unexpected rendering failure)
     */
    Result<DossierCertificationSnapshot, ApplicationError> handle(CertifyAgronomicDossierCommand command);
}
