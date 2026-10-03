package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.DossierCertificationResource;

/** Maps a stored certification to the public response; the PDF bytes are never part of it. */
public final class DossierCertificationResourceFromEntityAssembler {
    private DossierCertificationResourceFromEntityAssembler() { }

    /**
     * Maps the certification snapshot.
     *
     * @param certification stored certification
     * @return the response resource
     */
    public static DossierCertificationResource toResource(DossierCertificationSnapshot certification) {
        var metadata = certification.metadata();
        return new DossierCertificationResource(certification.id().certificationId(),
                certification.reportId().reportId(), certification.plotId().plotId(),
                certification.campaignYear().value(), metadata.verificationHash().value(),
                metadata.auditorSignature().value(), certification.certifier().name(),
                certification.certifier().cipNumber(), metadata.certifiedAt());
    }
}
