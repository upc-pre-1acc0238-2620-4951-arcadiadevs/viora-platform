package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;

import java.time.Year;

/**
 * Domain query requesting the sampling statuses of all active plots belonging to a producer for a campaign.
 *
 * @param producerId   producer identifier
 * @param campaignYear agricultural campaign year
 */
public record GetPlotSamplingStatesQuery(
        String producerId,
        CampaignYear campaignYear
) {

    /**
     * Compact constructor validating required producer and defaulting campaign year.
     */
    public GetPlotSamplingStatesQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("thinning.producer.null_or_empty");
        }
        if (campaignYear == null) {
            campaignYear = new CampaignYear(Year.now(ProducerTimeZone.ZONE).getValue());
        }
    }

    /**
     * Convenience constructor defaulting to the current calendar year.
     *
     * @param producerId producer identifier
     */
    public GetPlotSamplingStatesQuery(String producerId) {
        this(producerId, null);
    }
}
