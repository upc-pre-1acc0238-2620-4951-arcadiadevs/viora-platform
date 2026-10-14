package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;

/**
 * Domain query requesting the thinning prescriptions of every active plot of a producer for a campaign.
 *
 * @param producerId   producer identifier
 * @param campaignYear agricultural campaign year
 */
public record GetThinningPrescriptionsQuery(
        String producerId,
        CampaignYear campaignYear
) {

    public GetThinningPrescriptionsQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("thinning.producer.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
    }
}
