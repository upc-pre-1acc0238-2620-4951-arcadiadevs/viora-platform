package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Outbound ACL contract for phenology data consumed by the thinning context.
 *
 * <p>The current platform exposes BBI through Phenology metrics but does not yet publish a
 * dedicated cross-context contract for pit-hardening windows. Keeping this port inside Thinning
 * prevents the domain from importing Phenology aggregates and leaves that integration explicit.</p>
 */
public interface ExternalPhenologyService {

    /**
     * Loads the phenology inputs required for a thinning prescription.
     *
     * @param plotId target plot identifier
     * @param campaignYear target campaign year
     * @return isolated phenology data, or empty when the provider has no sufficient data
     */
    Optional<PhenologyPrescriptionInputs> fetchPrescriptionInputs(PlotId plotId, CampaignYear campaignYear);

    /**
     * Immutable ACL DTO containing only data relevant to Thinning.
     *
     * @param bbi             Biennial Bearing Index in [0, 1]
     * @param windowOpensOn   beginning of the recommended intervention window
     * @param windowClosesOn  end of the recommended intervention window
     */
    record PhenologyPrescriptionInputs(
            Double bbi,
            LocalDate windowOpensOn,
            LocalDate windowClosesOn
    ) {
        public PhenologyPrescriptionInputs {
            if (bbi == null || !Double.isFinite(bbi) || bbi < 0.0 || bbi > 1.0) {
                throw new IllegalArgumentException("thinning.bbi.invalid_range");
            }
            if (windowOpensOn != null && windowClosesOn != null && windowOpensOn.isAfter(windowClosesOn)) {
                throw new IllegalArgumentException("thinning.phenology.window.invalid");
            }
        }
    }
}
