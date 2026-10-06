package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Outbound ACL used by Settlement to name the commercial size grade of the caliber it settled. */
@Service("settlementExternalThinningService")
public class ExternalThinningService {

    private final ThinningContextFacade thinningContextFacade;

    /**
     * Constructs the outbound service injecting the Thinning ACL facade.
     *
     * @param thinningContextFacade the Thinning context facade
     */
    public ExternalThinningService(ThinningContextFacade thinningContextFacade) {
        this.thinningContextFacade = thinningContextFacade;
    }

    /**
     * Resolves the commercial size grade of a caliber.
     *
     * @param fruitsPerKg number of fruits per kilogram, or null when the caliber is unknown
     * @return the grade label, or empty when there is no caliber to grade
     */
    public Optional<String> findCommercialSizeGrade(Double fruitsPerKg) {
        if (fruitsPerKg == null) {
            return Optional.empty();
        }
        return thinningContextFacade.findCommercialSizeGrade(fruitsPerKg);
    }
}
