package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimate;
import com.arcadiadevs.viora.platform.thinning.domain.services.YieldEstimationService;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.YieldEstimateResource;

/** Maps a domain yield estimate to its REST resource, rounding only for display. */
public final class YieldEstimateResourceAssembler {

    private YieldEstimateResourceAssembler() {
    }

    /**
     * Maps a yield estimate.
     *
     * @param estimate domain estimate, or null
     * @return the resource, or null when there is no estimate
     */
    public static YieldEstimateResource toResource(YieldEstimate estimate) {
        if (estimate == null) {
            return null;
        }
        return new YieldEstimateResource(
                estimate.status().name(),
                ThinningRounding.kilograms(estimate.sustainableKgPerHa()),
                ThinningRounding.kilograms(estimate.estimatedKgPerHa()),
                estimate.basis() == null ? null : estimate.basis().name(),
                estimate.harvestCampaigns(),
                YieldEstimationService.MIN_HARVEST_CAMPAIGNS);
    }
}
