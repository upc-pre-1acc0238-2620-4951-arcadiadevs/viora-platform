package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.commands.CertifyAgronomicDossierCommand;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.CertifyDossierResource;

/** Maps HTTP input to the certification command. */
public final class CertifyAgronomicDossierCommandFromResourceAssembler {
    private CertifyAgronomicDossierCommandFromResourceAssembler() { }

    /**
     * Builds the command from the path and the body.
     *
     * @param plotId   plot of the path
     * @param resource request body
     * @return the certification command
     */
    public static CertifyAgronomicDossierCommand toCommand(String plotId, CertifyDossierResource resource) {
        return new CertifyAgronomicDossierCommand(plotId, resource.campaignYear(), resource.auditorSignature(),
                resource.certifiedBy(), resource.cipNumber(), resource.notes());
    }
}
